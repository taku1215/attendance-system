/**
 * Real-time Audio Chord Detector
 * Web Audio API + FFT + harmonic suppression + vote-based stabilization
 */

// ─── Note / Chord Definitions ────────────────────────────────────────────────

const NOTE_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];

const CHORD_TEMPLATES = [
  { name: '',      intervals: [4, 7]          },
  { name: 'm',     intervals: [3, 7]          },
  { name: 'dim',   intervals: [3, 6]          },
  { name: 'aug',   intervals: [4, 8]          },
  { name: 'sus2',  intervals: [2, 7]          },
  { name: 'sus4',  intervals: [5, 7]          },
  { name: 'maj7',  intervals: [4, 7, 11]      },
  { name: '7',     intervals: [4, 7, 10]      },
  { name: 'm7',    intervals: [3, 7, 10]      },
  { name: 'm7b5',  intervals: [3, 6, 10]      },
  { name: 'dim7',  intervals: [3, 6, 9]       },
  { name: 'add9',  intervals: [4, 7, 14]      },
];

// ─── Frequency Helpers ────────────────────────────────────────────────────────

function freqToNoteClass(freq) {
  const midi = Math.round(69 + 12 * Math.log2(freq / 440));
  return ((midi % 12) + 12) % 12;
}

// ─── Peak Detection with Harmonic Suppression ─────────────────────────────────

function findPeaks(floatFreqData, sampleRate, fftSize, thresholdRatio) {
  const binCount  = floatFreqData.length;
  const binFreq   = sampleRate / fftSize;
  const MIN_FREQ  = 55;   // A1 – lowest practical note
  const MAX_FREQ  = 2000; // limit to reduce false harmonics
  const minBin    = Math.ceil(MIN_FREQ / binFreq);
  const maxBin    = Math.min(Math.floor(MAX_FREQ / binFreq), binCount - 1);

  // dB → linear
  const lin = new Float32Array(binCount);
  for (let i = 0; i < binCount; i++) lin[i] = Math.pow(10, floatFreqData[i] / 20);

  // 3-point moving average
  const sm = new Float32Array(binCount);
  for (let i = 1; i < binCount - 1; i++) sm[i] = (lin[i-1] + lin[i]*2 + lin[i+1]) / 4;

  // dynamic noise floor
  let maxMag = 0;
  for (let i = minBin; i <= maxBin; i++) if (sm[i] > maxMag) maxMag = sm[i];
  const floor = maxMag * thresholdRatio;

  // local maxima
  const peaks = [];
  for (let i = minBin + 1; i < maxBin; i++) {
    if (sm[i] > sm[i-1] && sm[i] > sm[i+1] && sm[i] > floor) {
      const a = sm[i-1], b = sm[i], c = sm[i+1];
      const off = (a - c) / (2*(a - 2*b + c) + 1e-10);
      peaks.push({ freq: (i + off) * binFreq, mag: sm[i] });
    }
  }

  peaks.sort((a, b) => b.mag - a.mag);

  // Harmonic suppression: remove peaks that are 2x/3x/4x/5x of a stronger peak
  const keep = [];
  const suppressed = new Set();
  for (let i = 0; i < peaks.length; i++) {
    if (suppressed.has(i)) continue;
    keep.push(peaks[i]);
    for (let j = i + 1; j < peaks.length; j++) {
      if (suppressed.has(j)) continue;
      const ratio = peaks[j].freq / peaks[i].freq;
      for (let h = 2; h <= 5; h++) {
        if (Math.abs(ratio - h) / h < 0.04) { // 4% tolerance
          suppressed.add(j);
          break;
        }
      }
    }
  }

  return keep.slice(0, 6); // keep only top 6 fundamentals
}

// ─── Chord Matching ───────────────────────────────────────────────────────────

function matchChord(noteClasses) {
  if (noteClasses.length < 2) return [];

  const results = [];

  for (const root of noteClasses) {
    for (const tpl of CHORD_TEMPLATES) {
      const chordNotes = new Set([root, ...tpl.intervals.map(i => (root + i) % 12)]);
      const chordArr   = [...chordNotes];

      let hits = 0;
      for (const n of chordArr)   if (noteClasses.includes(n)) hits++;
      const coverage = hits / chordArr.length;
      if (coverage < 0.65) continue;

      // Extra note penalty – only count notes NOT in chord
      let extra = 0;
      for (const n of noteClasses) if (!chordNotes.has(n)) extra++;

      // Root presence bonus
      const rootPresent = noteClasses.includes(root) ? 1 : 0;

      const score = coverage * 10 + rootPresent * 3 - extra * 0.8;
      results.push({
        root: NOTE_NAMES[root],
        type: tpl.name,
        score,
        coverage,
        fullName: NOTE_NAMES[root] + tpl.name,
        chordNotes,
      });
    }
  }

  results.sort((a, b) => b.score - a.score);
  return results.slice(0, 5);
}

// ─── Vote Buffer for Stabilization ───────────────────────────────────────────

const VOTE_WINDOW = 5;
const voteBuffer = [];

function castVote(name) {
  voteBuffer.push(name);
  if (voteBuffer.length > VOTE_WINDOW) voteBuffer.shift();

  const counts = {};
  for (const v of voteBuffer) counts[v] = (counts[v] || 0) + 1;

  const [topName, topCount] = Object.entries(counts).sort((a, b) => b[1] - a[1])[0];
  if (topCount >= Math.ceil(VOTE_WINDOW * 0.5)) return topName;
  return null;
}

function resetVotes() { voteBuffer.length = 0; }

// ─── Audio / Visualizer Engine ────────────────────────────────────────────────

const FFT_SIZE    = 8192;
const SMOOTHING   = 0.82;
const DETECT_MS   = 75;

let audioCtx = null, analyser = null, mediaStream = null;
let animFrameId = null, detectIntervalId = null, isRunning = false;

const chordNameEl    = document.getElementById('chordName');
const notesDisplayEl = document.getElementById('notesDisplay');
const candidateListEl = document.getElementById('candidateList');
const startBtn       = document.getElementById('startBtn');
const stopBtn        = document.getElementById('stopBtn');
const statusDot      = document.getElementById('statusDot');
const statusText     = document.getElementById('statusText');
const freqCanvas     = document.getElementById('freqCanvas');
const waveCanvas     = document.getElementById('waveCanvas');
const slider         = document.getElementById('sensitivitySlider');
const sliderVal      = document.getElementById('sensitivityValue');

const freqCtx = freqCanvas.getContext('2d');
const waveCtx = waveCanvas.getContext('2d');

// Sensitivity slider: value 1→10 maps threshold 0.22→0.02
function getThreshold() {
  return 0.24 - parseInt(slider.value) * 0.022;
}

slider.addEventListener('input', () => { sliderVal.textContent = slider.value; });

// ─── Drawing ──────────────────────────────────────────────────────────────────

function resizeCanvas(c) {
  const r = c.parentElement.getBoundingClientRect();
  c.width  = r.width  * devicePixelRatio;
  c.height = c.offsetHeight * devicePixelRatio;
}

function drawBars(byteData) {
  resizeCanvas(freqCanvas);
  const w = freqCanvas.width, h = freqCanvas.height;
  freqCtx.clearRect(0, 0, w, h);
  const count = Math.min(byteData.length, 600);
  const bw = w / count;
  for (let i = 0; i < count; i++) {
    const v = byteData[i] / 255;
    const hue = 255 + (i / count) * 60;
    freqCtx.fillStyle = `hsla(${hue},80%,60%,0.9)`;
    freqCtx.fillRect(i * bw, h - v * h, bw - 0.5, v * h);
  }
}

function drawWave(byteData) {
  resizeCanvas(waveCanvas);
  const w = waveCanvas.width, h = waveCanvas.height;
  waveCtx.clearRect(0, 0, w, h);
  waveCtx.strokeStyle = '#7c3aed';
  waveCtx.lineWidth = 2;
  waveCtx.beginPath();
  const step = w / byteData.length;
  for (let i = 0; i < byteData.length; i++) {
    const y = (byteData[i] / 128) * (h / 2);
    i ? waveCtx.lineTo(i * step, y) : waveCtx.moveTo(0, y);
  }
  waveCtx.stroke();
}

// ─── Detection Loop ───────────────────────────────────────────────────────────

let silenceCount = 0;

function runDetection() {
  if (!analyser) return;

  const floatData = new Float32Array(analyser.frequencyBinCount);
  analyser.getFloatFrequencyData(floatData);

  const peaks = findPeaks(floatData, audioCtx.sampleRate, FFT_SIZE, getThreshold());

  if (peaks.length < 2) {
    silenceCount++;
    if (silenceCount > 8) {
      resetVotes();
      chordNameEl.className = 'chord-name no-signal';
      chordNameEl.textContent = '♩';
      notesDisplayEl.innerHTML = '';
      candidateListEl.innerHTML = '';
      silenceCount = 0;
    }
    return;
  }
  silenceCount = 0;

  // Deduplicate by note class, keep highest magnitude per class
  const ncMap = new Map();
  for (const p of peaks) {
    const nc = freqToNoteClass(p.freq);
    if (!ncMap.has(nc) || p.mag > ncMap.get(nc).mag) ncMap.set(nc, p);
  }

  // Sort note classes by magnitude (strongest first) and take top 5
  const noteClasses = [...ncMap.entries()]
    .sort((a, b) => b[1].mag - a[1].mag)
    .slice(0, 5)
    .map(([nc]) => nc);

  const candidates = matchChord(noteClasses);
  if (candidates.length === 0) { castVote('?'); return; }

  const best      = candidates[0];
  const displayed = castVote(best.fullName);
  if (!displayed) return; // not enough consensus yet

  // Update chord name
  chordNameEl.className = 'chord-name active';
  chordNameEl.textContent = displayed;

  // Show notes
  notesDisplayEl.innerHTML = '';
  for (const nc of noteClasses) {
    const chip = document.createElement('span');
    const isRoot = NOTE_NAMES[nc] === best.root;
    chip.className = 'note-chip' + (isRoot ? ' root' : '');
    chip.textContent = NOTE_NAMES[nc];
    notesDisplayEl.appendChild(chip);
  }

  // Show candidates
  candidateListEl.innerHTML = '';
  candidates.forEach((c, i) => {
    const chip = document.createElement('span');
    chip.className = 'candidate-chip' + (i < 3 ? ' top' : '');
    chip.textContent = c.fullName;
    candidateListEl.appendChild(chip);
  });
}

function animLoop() {
  if (!isRunning) return;
  const freq = new Uint8Array(analyser.frequencyBinCount);
  const wave = new Uint8Array(analyser.fftSize);
  analyser.getByteFrequencyData(freq);
  analyser.getByteTimeDomainData(wave);
  drawBars(freq);
  drawWave(wave);
  animFrameId = requestAnimationFrame(animLoop);
}

// ─── Start / Stop ─────────────────────────────────────────────────────────────

async function startDetection() {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
    audioCtx    = new (window.AudioContext || window.webkitAudioContext)();
    analyser    = audioCtx.createAnalyser();
    analyser.fftSize               = FFT_SIZE;
    analyser.smoothingTimeConstant = SMOOTHING;

    audioCtx.createMediaStreamSource(mediaStream).connect(analyser);

    isRunning = true;
    startBtn.disabled = true;
    stopBtn.disabled  = false;
    statusDot.className  = 'dot listening';
    statusText.textContent = '🎤 聴取中...';
    chordNameEl.className  = 'chord-name no-signal';
    chordNameEl.textContent = '♩';

    detectIntervalId = setInterval(runDetection, DETECT_MS);
    animLoop();
  } catch (err) {
    alert('マイクへのアクセスが拒否されました: ' + err.message);
  }
}

function stopDetection() {
  isRunning = false;
  clearInterval(detectIntervalId);
  cancelAnimationFrame(animFrameId);
  mediaStream?.getTracks().forEach(t => t.stop());
  audioCtx?.close();
  mediaStream = audioCtx = analyser = null;
  resetVotes();

  startBtn.disabled = false;
  stopBtn.disabled  = true;
  statusDot.className    = 'dot';
  statusText.textContent = '停止中';
  chordNameEl.className  = 'chord-name no-signal';
  chordNameEl.textContent = '-- / --';
  notesDisplayEl.innerHTML  = '';
  candidateListEl.innerHTML = '';
  freqCtx.clearRect(0, 0, freqCanvas.width, freqCanvas.height);
  waveCtx.clearRect(0, 0, waveCanvas.width, waveCanvas.height);
}

startBtn.addEventListener('click', startDetection);
stopBtn.addEventListener('click', stopDetection);
