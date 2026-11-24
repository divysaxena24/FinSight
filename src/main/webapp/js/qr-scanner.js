/**
 * FinSight QR Scanner & Transfer Engine
 * Supports Native BarcodeDetector, Canvas Image Processing, Camera Streaming & File Uploads
 */
(function(window) {
    'use strict';

    class FinSightQRScanner {
        constructor(options = {}) {
            this.videoElement = options.videoElement;
            this.overlayElement = options.overlayElement;
            this.onScanSuccess = options.onScanSuccess || function() {};
            this.onScanError = options.onScanError || function() {};
            this.stream = null;
            this.isScanning = false;
            this.animationFrameId = null;
            this.canvas = document.createElement('canvas');
            this.ctx = this.canvas.getContext('2d', { willReadFrequently: true });
            this.detector = null;
            this.initDetector();
        }

        async initDetector() {
            if ('BarcodeDetector' in window) {
                try {
                    const formats = await BarcodeDetector.getSupportedFormats();
                    if (formats.includes('qr_code')) {
                        this.detector = new BarcodeDetector({ formats: ['qr_code'] });
                    }
                } catch (e) {
                    console.warn('[FinSightQR] Native BarcodeDetector initialization warning:', e);
                }
            }
        }

        async startCamera() {
            if (this.isScanning) return;
            try {
                const constraints = {
                    video: {
                        facingMode: { ideal: 'environment' },
                        width: { ideal: 1280 },
                        height: { ideal: 720 }
                    },
                    audio: false
                };

                this.stream = await navigator.mediaDevices.getUserMedia(constraints);
                if (this.videoElement) {
                    this.videoElement.srcObject = this.stream;
                    this.videoElement.setAttribute('playsinline', 'true');
                    await this.videoElement.play();
                }
                this.isScanning = true;
                if (this.overlayElement) {
                    this.overlayElement.style.display = 'block';
                }
                this.scanLoop();
            } catch (err) {
                this.isScanning = false;
                console.error('[FinSightQR] Camera access error:', err);
                let message = 'Unable to access camera. Please check permissions or upload a QR image.';
                if (err.name === 'NotAllowedError') {
                    message = 'Camera permission denied. Please allow camera access in your browser or upload a QR code image.';
                } else if (err.name === 'NotFoundError') {
                    message = 'No camera found on this device. You can upload a QR image or select from Quick Payees.';
                }
                this.onScanError(message);
            }
        }

        stopCamera() {
            this.isScanning = false;
            if (this.animationFrameId) {
                cancelAnimationFrame(this.animationFrameId);
                this.animationFrameId = null;
            }
            if (this.stream) {
                this.stream.getTracks().forEach(track => track.stop());
                this.stream = null;
            }
            if (this.videoElement) {
                this.videoElement.srcObject = null;
            }
            if (this.overlayElement) {
                this.overlayElement.style.display = 'none';
            }
        }

        async scanLoop() {
            if (!this.isScanning) return;

            if (this.videoElement && this.videoElement.readyState >= 2) {
                try {
                    const text = await this.decodeFrame(this.videoElement);
                    if (text) {
                        this.playBeep();
                        this.stopCamera();
                        this.onScanSuccess(text);
                        return;
                    }
                } catch (err) {
                    // Continue scanning next frame
                }
            }

            this.animationFrameId = requestAnimationFrame(() => this.scanLoop());
        }

        async decodeFrame(source) {
            // 1. Try Native BarcodeDetector if available
            if (this.detector) {
                try {
                    const barcodes = await this.detector.detect(source);
                    if (barcodes && barcodes.length > 0 && barcodes[0].rawValue) {
                        return barcodes[0].rawValue.trim();
                    }
                } catch (e) {
                    // Fall back to jsQR
                }
            }

            // 2. Decode using bundled pure JavaScript jsQR engine
            if (window.jsQR) {
                try {
                    let width = source.videoWidth || source.naturalWidth || source.width;
                    let height = source.videoHeight || source.naturalHeight || source.height;
                    if (width && height) {
                        // Scale down oversized images for rapid, responsive decoding
                        const maxDim = 1200;
                        if (width > maxDim || height > maxDim) {
                            const scale = Math.min(maxDim / width, maxDim / height);
                            width = Math.max(1, Math.round(width * scale));
                            height = Math.max(1, Math.round(height * scale));
                        }
                        this.canvas.width = width;
                        this.canvas.height = height;
                        this.ctx.drawImage(source, 0, 0, width, height);
                        const imageData = this.ctx.getImageData(0, 0, width, height);
                        const code = window.jsQR(imageData.data, imageData.width, imageData.height, {
                            inversionAttempts: 'attemptBoth'
                        });
                        if (code && code.data && code.data.trim()) {
                            return code.data.trim();
                        }
                    }
                } catch (err) {
                    console.warn('[FinSightQR] jsQR frame processing error:', err);
                }
            }

            return null;
        }

        async decodeImageFile(file) {
            return new Promise((resolve, reject) => {
                if (!file || !file.type.startsWith('image/')) {
                    reject(new Error('Please select a valid image file.'));
                    return;
                }
                const reader = new FileReader();
                reader.onload = (e) => {
                    const img = new Image();
                    img.onload = async () => {
                        try {
                            const result = await this.decodeFrame(img);
                            if (result) {
                                this.playBeep();
                                resolve(result);
                            } else {
                                reject(new Error('No readable QR code found in this image. Please try a clearer image.'));
                            }
                        } catch (err) {
                            reject(err);
                        }
                    };
                    img.onerror = () => reject(new Error('Failed to load image file.'));
                    img.src = e.target.result;
                };
                reader.onerror = () => reject(new Error('Failed to read file.'));
                reader.readAsDataURL(file);
            });
        }

        playBeep() {
            try {
                const AudioCtx = window.AudioContext || window.webkitAudioContext;
                if (AudioCtx) {
                    const ctx = new AudioCtx();
                    const osc = ctx.createOscillator();
                    const gain = ctx.createGain();
                    osc.type = 'sine';
                    osc.frequency.setValueAtTime(880, ctx.currentTime); // A5 note
                    gain.gain.setValueAtTime(0.15, ctx.currentTime);
                    gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.18);
                    osc.connect(gain);
                    gain.connect(ctx.destination);
                    osc.start();
                    osc.stop(ctx.currentTime + 0.18);
                }
            } catch (e) {
                // Ignore audio autoplay restrictions
            }
        }
    }

    /**
     * FinSight QR Payment Payload Parser
     */
    const FinSightQRPayload = {
        parse: function(raw) {
            if (!raw || typeof raw !== 'string') return null;
            let str = raw.trim().replace(/^["']|["']$/g, '');

            // Format 1: JSON payload {"accountNumber":"FS-...","name":"...","amount":...}
            if (str.startsWith('{') && str.endsWith('}')) {
                try {
                    const obj = JSON.parse(str);
                    if (obj.accountNumber || obj.acc) {
                        return {
                            accountNumber: (obj.accountNumber || obj.acc).replace(/\s+/g, '').toUpperCase(),
                            name: obj.name || obj.holderName || '',
                            amount: obj.amount ? parseFloat(obj.amount) : null,
                            description: obj.description || obj.note || obj.desc || 'QR Instant Transfer'
                        };
                    }
                } catch (e) {}
            }

            // Format 2: finsight://pay?acc=FS-XXXXX&name=Alex&amount=500&desc=Lunch
            if (str.toLowerCase().startsWith('finsight://pay') || str.toLowerCase().startsWith('finsight:')) {
                const queryStr = str.includes('?') ? str.split('?')[1] : str;
                const params = new URLSearchParams(queryStr);
                const acc = params.get('acc') || params.get('account') || params.get('accountNumber');
                if (acc) {
                    return {
                        accountNumber: acc.replace(/\s+/g, '').toUpperCase(),
                        name: params.get('name') || '',
                        amount: params.get('am') || params.get('amount') ? parseFloat(params.get('am') || params.get('amount')) : null,
                        description: params.get('tn') || params.get('desc') || params.get('note') || 'FinSight QR Transfer'
                    };
                }
            }

            // Format 3: UPI standard URI: upi://pay?pa=FS-XXXXX&pn=Alex&am=500&tn=Dinner
            if (str.toLowerCase().startsWith('upi://pay')) {
                const queryStr = str.includes('?') ? str.split('?')[1] : '';
                const params = new URLSearchParams(queryStr);
                const pa = params.get('pa') || '';
                // pa could be FS-XXXXXXXX or FS-XXXXXXXX@finsight
                const cleanAcc = pa.replace(/@.*$/, '').replace(/\s+/g, '').toUpperCase();
                return {
                    accountNumber: cleanAcc,
                    name: params.get('pn') || '',
                    amount: params.get('am') ? parseFloat(params.get('am')) : null,
                    description: params.get('tn') || 'UPI QR Transfer'
                };
            }

            // Format 4: Match account pattern like FS-1234567890 or FS - 1234567890
            const fsMatch = str.match(/FS\s*-\s*([A-Za-z0-9\-_]+)/i);
            if (fsMatch) {
                return {
                    accountNumber: ('FS-' + fsMatch[1]).toUpperCase(),
                    name: '',
                    amount: null,
                    description: 'Direct QR Transfer'
                };
            }

            // Fallback: If it's a non-empty alphanumeric string
            return {
                accountNumber: str.replace(/\s+/g, '').toUpperCase(),
                name: '',
                amount: null,
                description: 'QR Transfer'
            };
        },

        buildURI: function(accountNumber, name, amount, description) {
            let uri = `finsight://pay?acc=${encodeURIComponent(accountNumber.trim())}`;
            if (name && name.trim()) uri += `&name=${encodeURIComponent(name.trim())}`;
            if (amount && Number(amount) > 0) uri += `&amount=${encodeURIComponent(amount)}`;
            if (description && description.trim()) uri += `&desc=${encodeURIComponent(description.trim())}`;
            return uri;
        }
    };

    window.FinSightQRScanner = FinSightQRScanner;
    window.FinSightQRPayload = FinSightQRPayload;
})(window);
