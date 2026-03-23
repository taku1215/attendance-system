/**
 * Real-time Audio Chord Detector
 * Uses Web Audio API + FFT for frequency analysis and chord recognition
 */

// ─── Note / Chord Definitions ────────────────────────────────────────────────

const NOTE_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];

// Chord templates: semitone intervals from root (excluding root = 0)
const CHORD_TEMPLATES = [
  // Triads
  { name: '',      intervals: [4, 7],       quality: 'major' },
  { name: 'm',     intervals: [3, 7],       quality: 'minor' },
  { name: 'dim',   intervals: [3, 6],       quality: 'dim' },
  { name: 'aug',   intervals: [4, 8],       quality: 'aug' },
  { name: 'sus2',  intervals: [2, 7],       quality: 'sus' },
  { name: 'sus4',  intervals: [5, 7],       quality: 'sus' },
  // 7ths
  { name: 'maj7',  intervals: [4, 7, 11],   quality: 'major' },
  { name: '7',     intervals: [4, 7, 10],   quality: 'dominant' },
  { name: 'm7',    intervals: [3, 7, 10],   quality: 'minor' },
  { name: 'm7b5',  intervals: [3, 6, 10],   quality: 'dim' },
  { name: 'dim7',  intervals: [3, 6, 9],    quality: 'dim' },
  { name: 'mmaj7', intervals: [3, 7, 11],   quality: 'minor' },
  { name: 'add9',  intervals: [4, 7, 14],   quality: 'major' },
  { name: 'm9',    intervals: [3, 7, 10, 14], quality: 'minor' },
  { name: '9',     intervals: [4, 7, 10, 14], quality: 'dominant' },
];

// ─── Frequency → Note conversion ─────────────────────────────────────────────

function freqToMidi(freq) {
  return 69 + 12 * Math.log2(freq / 440);
}

function freqToNoteName(freq) {
  const midi = Math.round(freqToMidi(freq));
  return NOTE_NAMES[((midi % 12) + 12) % 12];
}

function freqToNoteClass(freq) {
  const midi = Math.round(freqToMidi(freq));
  return ((midi % 12) + 12) % 12; // 0=C, 1=C#, ...
}

// ─── Peak Detection ───────────────────────────────────────────────────────────

/**
 * Find spectral peaks using Harmonic Product Spectrum (HPS)
 * Returns array of { freq, magnitude } sorted by magnitude descending
 */
function findSpectralPeaks(freqData, sampleRate, fftSize, threshold) {
  const binCount = freqData.length;
  const binFreq = sampleRate / fftSize;
  const peaks = [];

  // Convert dB to linear magnitude
  const linear = new Float32Array(binCount);
  for (let i = 0; i < binCount; i++) {
    linear[i] = Math.pow(10, freqData[i] / 20);
  }

  const minFreq = 60;   // Hz - below this is noise
  const maxFreq = 4000; // Hz - above this less relevant for chords
  const minBin = Math.ceil(minFreq / binFreq);
  const maxBin = Math.min(Math.floor(maxFreq / binFreq), binCount - 1);

  // Smooth the spectrum slightly
  const smoothed = new Float32Array(binCount);
  for (let i = 1; i < binCount - 1; i++) {
    smoothed[i] = (linear[i - 1] + linear[i] * 2 + linear[i + 1]) / 4;
  }

  // Dynamic noise floor
  let maxMag = 0;
  for (let i = minBin; i <= maxBin; i++) {
    if (smoothed[i] > maxMag) maxMag = smoothed[i];
  }

  const noiseFloor = maxMag * threshold;

  // Find local maxima
  for (let i = minBin + 1; i < maxBin; i++) {
    if (
      smoothed[i] > smoothed[i - 1] &&
      smoothed[i] > smoothed[i + 1] &&
      smoothed[i] > noiseFloor
    ) {
      // Parabolic interpolation for sub-bin accuracy
      const alpha = smoothed[i - 1];
      const beta  = smoothed[i];
      const gamma = smoothed[i + 1];
      const offset = (alpha - gamma) / (2 * (alpha - 2 * beta + gamma) + 1e-10);
      const exactBin = i + offset;
      const freq = exactBin * binFreq;

      if (freq >= minFreq && freq <= maxFreq) {
        peaks.push({ freq, magnitude: smoothed[i] });
      }
    }
  }

  // Sort by magnitude and return top peaks
  peaks.sort((a, b) => b.magnitude - a.magnitude);
  return peaks.slice(0, 12);
}

// ─── Chord Matching ───────────────────────────────────────────────────────────

function matchChord(noteClasses) {
  if (noteClasses.length < 2) return [];

  const results = [];

  // Try each detected note as root
  for (const rootClass of noteClasses) {
    for (const template of CHORD_TEMPLATES) {
      let score = 0;
      let matched = 0;
      let penalties = 0;

      const requiredNotes = new Set([rootClass]);
      for (const interval of template.intervals) {
        requiredNotes.add((rootClass + interval) % 12);
      }

      // Score: reward presence of chord tones, penalize missing ones
      for (const required of requiredNotes) {
        if (noteClasses.includes(required)) {
          matched++;
          score += (required === rootClass) ? 2 : 1;
        } else {
          penalties++;
        }
      }

      // Penalize extra notes not in chord
      for (const detected of noteClasses) {
        if (!requiredNotes.has(detected)) {
          score -= 0.5;
        }
      }

      const coverage = matched / requiredNotes.size;
      if (coverage >= 0.6) {
        results.push({
          root: NOTE_NAMES[rootClass],
          type: template.name,
          quality: template.quality,
          score: score - penalties * 0.3,
          coverage,
          fullName: NOTE_NAMES[rootClass] + template.name,
        });
      }
    }
  }

  results.sort((a, b) => b.score - a.score);
  return results.slice(0, 5);
}

// ─── Audio Engine ─────────────────────────────────────────────────────────────

const FFT_SIZE = 8192;
const SMOOTHING = 0.75;
const DETECT_INTERVAL_MS = 80;

let audioCtx = null;
let analyser = null;
let mediaStream = null;
let animFrameId = null;
let detectIntervalId = null;
let isRunning = false;

// DOM refs
const chordNameEl   = document.getElementById('chordName');
const notesDisplayEl = document.getElementById('notesDisplay');
const candidateListEl = document.getElementById('candidateList');
const startBtn      = document.getElementById('startBtn');
const stopBtn       = document.getElementById('stopBtn');
const statusDot     = document.getElementById('statusDot');
const statusText    = document.getElementById('statusText');
const freqCanvas    = document.getElementById('freqCanvas');
const waveCanvas    = document.getElementById('waveCanvas');
const slider        = document.getElementById('sensitivitySlider');
const sliderVal     = document.getElementById('sensitivityValue');

const freqCtx  = freqCanvas.getContext('2d');
const waveCtx  = waveCanvas.getContext('2d');

// Sensitivity: maps 1-10 to threshold 0.25 (low sensitivity) → 0.02 (high)
function getSensitivityThreshold() {
  const v = parseInt(slider.value);
  return 0.27 - v * 0.025;
}

slider.addEventListener('input', () => {
  sliderVal.textContent = slider.value;
});

// ─── Visualization ────────────────────────────────────────────────────────────

function resizeCanvas(canvas) {
  const rect = canvas.parentElement.getBoundingClientRect();
  canvas.width  = rect.width * devicePixelRatio;
  canvas.height = canvas.offsetHeight * devicePixelRatio;
}

function drawFreqBars(freqData) {
  resizeCanvas(freqCanvas);
  const w = freqCanvas.width;
  const h = freqCanvas.height;
  freqCtx.clearRect(0, 0, w, h);

  const binCount = Math.min(freqData.length, 512);
  const barW = w / binCount;

  for (let i = 0; i < binCount; i++) {
    const val = (freqData[i] + 140) / 140; // normalize from dB
    const barH = Math.max(0, val * h);

    const hue = 260 + i / binCount * 60;
    freqCtx.fillStyle = `hsla(${hue}, 80%, 60%, 0.85)`;
    freqCtx.fillRect(i * barW, h - barH, barW - 0.5, barH);
  }
}

function drawWaveform(timeData) {
  resizeCanvas(waveCanvas);
  const w = waveCanvas.width;
  const h = waveCanvas.height;
  waveCtx.clearRect(0, 0, w, h);

  waveCtx.strokeStyle = '#7c3aed';
  waveCtx.lineWidth = 2;
  waveCtx.beginPath();

  const slice = w / timeData.length;
  let x = 0;
  for (let i = 0; i < timeData.length; i++) {
    const v = timeData[i] / 128.0;
    const y = (v * h) / 2;
    i === 0 ? waveCtx.moveTo(x, y) : waveCtx.lineTo(x, y);
    x += slice;
  }
  waveCtx.stroke();
}

// ─── Detection Loop ───────────────────────────────────────────────────────────

let lastChordName = '';
let stableCount = 0;
const STABLE_THRESHOLD = 2; // require N consecutive same detections

function runDetection() {
  if (!analyser) return;

  const freqData = new Float32Array(analyser.frequencyBinCount);
  analyser.getFloatFrequencyData(freqData);

  const threshold = getSensitivityThreshold();
  const peaks = findSpectralPeaks(freqData, audioCtx.sampleRate, FFT_SIZE, threshold);

  if (peaks.length < 2) {
    if (++stableCount > 5) {
      chordNameEl.className = 'chord-name no-signal';
      chordNameEl.textContent = '♩';
      notesDisplayEl.innerHTML = '';
      candidateListEl.innerHTML = '';
      lastChordName = '';
      stableCount = 0;
    }
    return;
  }

  // Group peaks into note classes
  const noteClassMap = new Map();
  for (const peak of peaks) {
    if (peak.freq < 60 || peak.freq > 4000) continue;
    const nc = freqToNoteClass(peak.freq);
    const existing = noteClassMap.get(nc);
    if (!existing || peak.magnitude > existing.magnitude) {
      noteClassMap.set(nc, peak);
    }
  }

  const noteClasses = [...noteClassMap.keys()];
  const candidates = matchChord(noteClasses);

  if (candidates.length === 0) {
    chordNameEl.className = 'chord-name no-signal';
    chordNameEl.textContent = '?';
    return;
  }

  const best = candidates[0];

  // Stability filter
  if (best.fullName !== lastChordName) {
    stableCount = 1;
    lastChordName = best.fullName;
    return;
  }
  stableCount++;
  if (stableCount < STABLE_THRESHOLD) return;

  // Update chord display
  chordNameEl.className = 'chord-name active';
  chordNameEl.textContent = best.fullName;

  // Show detected notes
  notesDisplayEl.innerHTML = '';
  const chordNoteClasses = new Set([NOTE_NAMES.indexOf(best.root)]);
  // Reconstruct chord notes for highlighting
  const tmpl = CHORD_TEMPLATES.find(t => t.name === best.type);
  if (tmpl) {
    const rootIdx = NOTE_NAMES.indexOf(best.root);
    for (const interval of tmpl.intervals) {
      chordNoteClasses.add((rootIdx + interval) % 12);
    }
  }

  const shownNotes = new Set();
  for (const [nc] of noteClassMap) {
    if (shownNotes.has(nc)) continue;
    shownNotes.add(nc);
    const chip = document.createElement('span');
    chip.className = 'note-chip' + (nc === NOTE_NAMES.indexOf(best.root) ? ' root' : '');
    chip.textContent = NOTE_NAMES[nc];
    notesDisplayEl.appendChild(chip);
  }

  // Show candidates
  candidateListEl.innerHTML = '';
  candidates.forEach((c, idx) => {
    const chip = document.createElement('span');
    chip.className = 'candidate-chip' + (idx < 3 ? ' top' : '');
    chip.textContent = c.fullName;
    candidateListEl.appendChild(chip);
  });
}

function animationLoop() {
  if (!isRunning) return;

  const freqData = new Uint8Array(analyser.frequencyBinCount);
  const timeData = new Uint8Array(analyser.fftSize);
  analyser.getByteFrequencyData(freqData);
  analyser.getByteTimeDomainData(timeData);

  drawFreqBars(new Float32Array(freqData).map(v => (v / 128) * 70 - 70));
  drawWaveform(timeData);

  animFrameId = requestAnimationFrame(animationLoop);
}

// ─── Start / Stop ─────────────────────────────────────────────────────────────

async function startDetection() {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });

    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    analyser = audioCtx.createAnalyser();
    analyser.fftSize = FFT_SIZE;
    analyser.smoothingTimeConstant = SMOOTHING;

    const source = audioCtx.createMediaStreamSource(mediaStream);
    source.connect(analyser);

    isRunning = true;
    startBtn.disabled = true;
    stopBtn.disabled = false;
    statusDot.className = 'dot listening';
    statusText.textContent = '🎤 聴取中...';
    chordNameEl.className = 'chord-name no-signal';
    chordNameEl.textContent = '♩';

    detectIntervalId = setInterval(runDetection, DETECT_INTERVAL_MS);
    animationLoop();
  } catch (err) {
    alert('マイクへのアクセスが拒否されました: ' + err.message);
  }
}

function stopDetection() {
  isRunning = false;
  clearInterval(detectIntervalId);
  cancelAnimationFrame(animFrameId);

  if (mediaStream) {
    mediaStream.getTracks().forEach(t => t.stop());
    mediaStream = null;
  }
  if (audioCtx) {
    audioCtx.close();
    audioCtx = null;
    analyser = null;
  }

  startBtn.disabled = false;
  stopBtn.disabled = true;
  statusDot.className = 'dot';
  statusText.textContent = '停止中';
  chordNameEl.className = 'chord-name no-signal';
  chordNameEl.textContent = '-- / --';
  notesDisplayEl.innerHTML = '';
  candidateListEl.innerHTML = '';

  freqCtx.clearRect(0, 0, freqCanvas.width, freqCanvas.height);
  waveCtx.clearRect(0, 0, waveCanvas.width, waveCanvas.height);
}

startBtn.addEventListener('click', startDetection);
stopBtn.addEventListener('click', stopDetection);
