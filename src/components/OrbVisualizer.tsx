/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef } from 'react';
import { AssistantState, OrbSettings, OrbStyleType } from '../types/myra';

interface OrbVisualizerProps {
  state: AssistantState;
  settings: OrbSettings;
  audioLevel?: number; // 0.0 to 1.0
  onClick?: () => void;
}

export const OrbVisualizer: React.FC<OrbVisualizerProps> = ({
  state,
  settings,
  audioLevel = 0,
  onClick,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;
    let time = 0;

    // Palette definition based on theme and voice state
    const getColors = () => {
      // High contrast override during active voice events
      if (state === 'listening') {
        return {
          core: '#ffffff',
          mid: '#f43f5e',
          outer: '#881337',
          particle: '#fda4af',
          glow: 'rgba(244, 63, 94, ',
        };
      }
      if (state === 'speaking') {
        return {
          core: '#ffffff',
          mid: '#10b981',
          outer: '#064e3b',
          particle: '#6ee7b7',
          glow: 'rgba(16, 185, 129, ',
        };
      }
      if (state === 'thinking') {
        return {
          core: '#ffffff',
          mid: '#f59e0b',
          outer: '#78350f',
          particle: '#fde68a',
          glow: 'rgba(245, 158, 11, ',
        };
      }

      switch (settings.theme) {
        case 'cyber_purple':
          return {
            core: '#f5d0fe',
            mid: '#c084fc',
            outer: '#581c87',
            particle: '#f472b6',
            glow: 'rgba(168, 85, 247, ',
          };
        case 'romantic_rose':
          return {
            core: '#fbcfe8',
            mid: '#f43f5e',
            outer: '#be123c',
            particle: '#fda4af',
            glow: 'rgba(244, 63, 94, ',
          };
        case 'emerald_neon':
          return {
            core: '#a7f3d0',
            mid: '#10b981',
            outer: '#064e3b',
            particle: '#34d399',
            glow: 'rgba(16, 185, 129, ',
          };
        case 'cyber_amber':
          return {
            core: '#fef08a',
            mid: '#f59e0b',
            outer: '#78350f',
            particle: '#fde047',
            glow: 'rgba(245, 158, 11, ',
          };
        case 'myra_signature':
        case 'electric_blue':
        default:
          return {
            core: '#e0f2fe',
            mid: '#00e5ff',
            outer: '#1e3a8a',
            particle: '#38bdf8',
            glow: 'rgba(0, 229, 255, ',
          };
      }
    };

    const particleCount = settings.particleCount || 24;
    const particles = Array.from({ length: particleCount }, (_, i) => ({
      angle: (i / particleCount) * Math.PI * 2,
      radiusRatio: 0.5 + Math.random() * 0.7,
      speed: (0.4 + Math.random() * 0.6) * (i % 2 === 0 ? 1 : -1),
      size: 1.5 + Math.random() * 2.5,
      opacity: 0.3 + Math.random() * 0.7,
    }));

    const render = () => {
      const speedMultiplier = (settings.animationSpeed || 1.0);
      time += 0.025 * speedMultiplier;

      const width = canvas.width;
      const height = canvas.height;
      const centerX = width / 2;
      const centerY = height / 2;
      const baseRadius = Math.min(width, height) * 0.3 * (settings.sizeDp ? settings.sizeDp / 240 : 1.0);

      ctx.clearRect(0, 0, width, height);

      const colors = getColors();
      const style: OrbStyleType = settings.style || 'myra_signature';
      const glowIntensity = settings.glowIntensity || 0.85;
      const ringThickness = settings.ringThickness || 3.0;
      const opacity = settings.opacity !== undefined ? settings.opacity : 0.95;

      const reactiveBoost = (state === 'speaking' || state === 'listening')
        ? Math.max(audioLevel * 0.35, Math.abs(Math.sin(time * 6)) * 0.12)
        : 0;

      const activeRadius = baseRadius * (1 + Math.sin(time * 2) * 0.04 + reactiveBoost);

      ctx.save();
      ctx.globalAlpha = opacity;

      switch (style) {
        // -------------------------------------------------------------
        // 1. CLASSIC GLOW ORB
        // -------------------------------------------------------------
        case 'classic_glow': {
          const outerRadius = activeRadius * (1.3 + 0.3 * glowIntensity);
          const outerGrad = ctx.createRadialGradient(centerX, centerY, activeRadius * 0.3, centerX, centerY, outerRadius);
          outerGrad.addColorStop(0, `${colors.glow}${0.45 * glowIntensity})`);
          outerGrad.addColorStop(0.7, `${colors.glow}${0.1 * glowIntensity})`);
          outerGrad.addColorStop(1, `${colors.glow}0)`);
          ctx.fillStyle = outerGrad;
          ctx.beginPath();
          ctx.arc(centerX, centerY, outerRadius, 0, Math.PI * 2);
          ctx.fill();

          const coreGrad = ctx.createRadialGradient(
            centerX - activeRadius * 0.2, centerY - activeRadius * 0.2, 0,
            centerX, centerY, activeRadius
          );
          coreGrad.addColorStop(0, colors.core);
          coreGrad.addColorStop(0.4, colors.mid);
          coreGrad.addColorStop(1, colors.outer);
          ctx.fillStyle = coreGrad;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius, 0, Math.PI * 2);
          ctx.fill();

          // Specular Crescent Arc
          ctx.strokeStyle = 'rgba(255, 255, 255, 0.6)';
          ctx.lineWidth = ringThickness * 1.2;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.88, Math.PI * 1.1, Math.PI * 1.6);
          ctx.stroke();
          break;
        }

        // -------------------------------------------------------------
        // 2. SIRI-STYLE FLUID ORB
        // -------------------------------------------------------------
        case 'siri_fluid': {
          for (let layer = 0; layer < 2; layer++) {
            const phase = layer === 0 ? time * 2 : -time * 2.5;
            const lScale = layer === 0 ? 1.0 : 0.88;
            ctx.beginPath();
            const steps = 60;
            for (let i = 0; i <= steps; i++) {
              const a = (i / steps) * Math.PI * 2;
              const r = activeRadius * lScale * (
                1 + Math.sin(a * 3 + phase) * 0.12 + Math.cos(a * 5 - phase * 0.8) * 0.08 + reactiveBoost * 0.25
              );
              const px = centerX + Math.cos(a) * r;
              const py = centerY + Math.sin(a) * r;
              if (i === 0) ctx.moveTo(px, py);
              else ctx.lineTo(px, py);
            }
            ctx.closePath();
            const grad = ctx.createRadialGradient(
              centerX - activeRadius * 0.15, centerY - activeRadius * 0.15, 0,
              centerX, centerY, activeRadius * 1.1
            );
            grad.addColorStop(0, colors.core);
            grad.addColorStop(0.5, colors.mid);
            grad.addColorStop(1, colors.outer);
            ctx.fillStyle = grad;
            ctx.fill();
          }
          break;
        }

        // -------------------------------------------------------------
        // 3. NEON RING ORB
        // -------------------------------------------------------------
        case 'neon_ring': {
          // Center Core
          ctx.fillStyle = colors.mid;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.35, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = colors.core;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.18, 0, Math.PI * 2);
          ctx.fill();

          // Main Neon Ring
          ctx.strokeStyle = colors.mid;
          ctx.lineWidth = ringThickness * 1.4;
          ctx.shadowColor = colors.mid;
          ctx.shadowBlur = 15 * glowIntensity;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.75, 0, Math.PI * 2);
          ctx.stroke();

          // Outer Rotating Arc
          ctx.save();
          ctx.translate(centerX, centerY);
          ctx.rotate(time * 1.5);
          ctx.strokeStyle = colors.core;
          ctx.lineWidth = ringThickness * 1.2;
          ctx.beginPath();
          ctx.arc(0, 0, activeRadius, 0, Math.PI * 0.7);
          ctx.stroke();
          ctx.beginPath();
          ctx.arc(0, 0, activeRadius, Math.PI, Math.PI * 1.4);
          ctx.stroke();
          ctx.restore();
          break;
        }

        // -------------------------------------------------------------
        // 4. GALAXY PARTICLE ORB
        // -------------------------------------------------------------
        case 'galaxy_particle': {
          ctx.fillStyle = colors.outer;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.45, 0, Math.PI * 2);
          ctx.fill();

          const arms = 3;
          const count = Math.min(particles.length, 48);
          for (let i = 0; i < count; i++) {
            const arm = i % arms;
            const progress = i / count;
            const spiralAngle = (progress * 3.5 * Math.PI + (arm * 2 * Math.PI / arms)) + time;
            const dist = activeRadius * 0.4 + progress * activeRadius * 0.85 * (1 + reactiveBoost);
            const px = centerX + Math.cos(spiralAngle) * dist;
            const py = centerY + Math.sin(spiralAngle) * dist;

            ctx.fillStyle = i % 2 === 0 ? colors.core : colors.mid;
            ctx.beginPath();
            ctx.arc(px, py, (2 + (1 - progress) * 2.5) * (1 + reactiveBoost * 0.5), 0, Math.PI * 2);
            ctx.fill();
          }
          break;
        }

        // -------------------------------------------------------------
        // 5. ENERGY CORE ORB
        // -------------------------------------------------------------
        case 'energy_core': {
          // Outer containment ring
          ctx.strokeStyle = colors.mid;
          ctx.lineWidth = ringThickness;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 1.1, 0, Math.PI * 2);
          ctx.stroke();

          // Energy spokes
          const spokes = 8;
          for (let s = 0; s < spokes; s++) {
            const ang = (s / spokes) * Math.PI * 2 + time * 1.2;
            const x1 = centerX + Math.cos(ang) * (activeRadius * 0.4);
            const y1 = centerY + Math.sin(ang) * (activeRadius * 0.4);
            const x2 = centerX + Math.cos(ang) * (activeRadius * 1.1);
            const y2 = centerY + Math.sin(ang) * (activeRadius * 1.1);
            ctx.strokeStyle = s % 2 === 0 ? '#ffffff' : colors.mid;
            ctx.lineWidth = ringThickness * (s % 2 === 0 ? 1.5 : 1.0);
            ctx.beginPath();
            ctx.moveTo(x1, y1);
            ctx.lineTo(x2, y2);
            ctx.stroke();
          }

          // Central Singularity
          const singGrad = ctx.createRadialGradient(centerX, centerY, 0, centerX, centerY, activeRadius * 0.45);
          singGrad.addColorStop(0, '#ffffff');
          singGrad.addColorStop(0.5, colors.mid);
          singGrad.addColorStop(1, '#05070e');
          ctx.fillStyle = singGrad;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.45 * (1 + reactiveBoost * 0.4), 0, Math.PI * 2);
          ctx.fill();
          break;
        }

        // -------------------------------------------------------------
        // 6. AURORA ORB
        // -------------------------------------------------------------
        case 'aurora': {
          // Atmospheric background
          const auroraGrad = ctx.createRadialGradient(centerX, centerY, activeRadius * 0.2, centerX, centerY, activeRadius * 1.2);
          auroraGrad.addColorStop(0, `${colors.glow}0.4)`);
          auroraGrad.addColorStop(1, 'transparent');
          ctx.fillStyle = auroraGrad;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 1.2, 0, Math.PI * 2);
          ctx.fill();

          // Aurora undulating waves
          for (let i = -2; i <= 2; i++) {
            const yOffset = i * (activeRadius * 0.28);
            ctx.strokeStyle = i % 2 === 0 ? colors.core : colors.mid;
            ctx.lineWidth = ringThickness * 1.8;
            ctx.beginPath();
            const steps = 30;
            const stepW = (activeRadius * 2) / steps;
            for (let s = 0; s <= steps; s++) {
              const px = centerX - activeRadius + s * stepW;
              const wave = Math.sin(s * 0.35 + time * 2 + i) * (activeRadius * 0.12 * (1 + reactiveBoost));
              const py = centerY + yOffset + wave;
              if (s === 0) ctx.moveTo(px, py);
              else ctx.lineTo(px, py);
            }
            ctx.stroke();
          }
          break;
        }

        // -------------------------------------------------------------
        // 7. GLASS ORB
        // -------------------------------------------------------------
        case 'glass': {
          // Translucent glass body
          const glassGrad = ctx.createLinearGradient(
            centerX - activeRadius, centerY - activeRadius,
            centerX + activeRadius, centerY + activeRadius
          );
          glassGrad.addColorStop(0, 'rgba(255, 255, 255, 0.4)');
          glassGrad.addColorStop(0.5, `${colors.glow}0.25)`);
          glassGrad.addColorStop(1, 'rgba(10, 16, 29, 0.8)');
          ctx.fillStyle = glassGrad;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius, 0, Math.PI * 2);
          ctx.fill();

          // Inner suspended nucleus
          ctx.fillStyle = colors.core;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.35 * (1 + reactiveBoost * 0.3), 0, Math.PI * 2);
          ctx.fill();

          // Sharp glass rim
          ctx.strokeStyle = 'rgba(255, 255, 255, 0.7)';
          ctx.lineWidth = ringThickness;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius, 0, Math.PI * 2);
          ctx.stroke();

          // Crescent glare
          ctx.strokeStyle = '#ffffff';
          ctx.lineWidth = ringThickness * 1.4;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.85, Math.PI * 1.15, Math.PI * 1.55);
          ctx.stroke();
          break;
        }

        // -------------------------------------------------------------
        // 8. PULSE ORB
        // -------------------------------------------------------------
        case 'pulse': {
          const waves = 3;
          for (let i = 0; i < waves; i++) {
            const fraction = ((time * 0.8 + (i / waves)) % 1);
            const wRadius = activeRadius * 0.4 + fraction * activeRadius * 0.9 * (1 + reactiveBoost);
            const alpha = (1 - fraction) * 0.8;
            ctx.strokeStyle = `${colors.glow}${alpha})`;
            ctx.lineWidth = ringThickness * (1.5 - fraction * 0.5);
            ctx.beginPath();
            ctx.arc(centerX, centerY, wRadius, 0, Math.PI * 2);
            ctx.stroke();
          }

          // Dense Nucleus
          ctx.fillStyle = colors.core;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.4 * (1 + reactiveBoost * 0.4), 0, Math.PI * 2);
          ctx.fill();
          break;
        }

        // -------------------------------------------------------------
        // 9. WAVE ORB
        // -------------------------------------------------------------
        case 'wave': {
          ctx.fillStyle = 'rgba(10, 15, 29, 0.4)';
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius, 0, Math.PI * 2);
          ctx.fill();

          const lines = 7;
          for (let i = -3; i <= 3; i++) {
            const norm = i / 3;
            const chord = Math.sqrt(Math.max(0, 1 - norm * norm)) * activeRadius * 2;
            const py = centerY + norm * activeRadius * 0.85;
            ctx.strokeStyle = i === 0 ? '#ffffff' : colors.mid;
            ctx.lineWidth = ringThickness * (i === 0 ? 1.4 : 1.0);
            ctx.beginPath();
            const steps = 24;
            const stepW = chord / steps;
            const startX = centerX - chord / 2;
            for (let s = 0; s <= steps; s++) {
              const px = startX + s * stepW;
              const sineVal = Math.sin(s * 0.6 + time * 3 + i) * (activeRadius * 0.09 * (1 + reactiveBoost * 0.8));
              if (s === 0) ctx.moveTo(px, py + sineVal);
              else ctx.lineTo(px, py + sineVal);
            }
            ctx.stroke();
          }

          ctx.strokeStyle = colors.mid;
          ctx.lineWidth = ringThickness;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius, 0, Math.PI * 2);
          ctx.stroke();
          break;
        }

        // -------------------------------------------------------------
        // 10. MYRA SIGNATURE ORB (Flagship Gyroscope + Quantum Constellation)
        // -------------------------------------------------------------
        case 'myra_signature':
        default: {
          // 1. Quantum Diffuse Aura
          const sigGrad = ctx.createRadialGradient(centerX, centerY, activeRadius * 0.3, centerX, centerY, activeRadius * 1.4);
          sigGrad.addColorStop(0, `${colors.glow}${0.5 * glowIntensity})`);
          sigGrad.addColorStop(0.7, `${colors.glow}${0.1 * glowIntensity})`);
          sigGrad.addColorStop(1, 'transparent');
          ctx.fillStyle = sigGrad;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 1.4, 0, Math.PI * 2);
          ctx.fill();

          // 2. High-Tech Core
          const coreGrad = ctx.createRadialGradient(
            centerX - activeRadius * 0.15, centerY - activeRadius * 0.15, 0,
            centerX, centerY, activeRadius * 0.7
          );
          coreGrad.addColorStop(0, '#ffffff');
          coreGrad.addColorStop(0.4, colors.mid);
          coreGrad.addColorStop(1, colors.outer);
          ctx.fillStyle = coreGrad;
          ctx.beginPath();
          ctx.arc(centerX, centerY, activeRadius * 0.68, 0, Math.PI * 2);
          ctx.fill();

          // 3. Dual Gyroscopic Orbital Rings
          ctx.save();
          ctx.translate(centerX, centerY);
          ctx.rotate(time * 1.2);
          ctx.strokeStyle = colors.mid;
          ctx.lineWidth = ringThickness * 1.2;
          ctx.beginPath();
          ctx.ellipse(0, 0, activeRadius * 1.15, activeRadius * 0.45, 0, 0, Math.PI * 2);
          ctx.stroke();
          ctx.fillStyle = '#ffffff';
          ctx.beginPath();
          ctx.arc(activeRadius * 1.15, 0, ringThickness * 1.6, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();

          ctx.save();
          ctx.translate(centerX, centerY);
          ctx.rotate(-time * 1.4 + 1.0);
          ctx.strokeStyle = colors.core;
          ctx.lineWidth = ringThickness * 1.2;
          ctx.beginPath();
          ctx.ellipse(0, 0, activeRadius * 1.15, activeRadius * 0.45, 0, 0, Math.PI * 2);
          ctx.stroke();
          ctx.fillStyle = '#ffffff';
          ctx.beginPath();
          ctx.arc(-activeRadius * 1.15, 0, ringThickness * 1.6, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();

          // 4. 8-Node Constellation
          const nodes = 8;
          for (let n = 0; n < nodes; n++) {
            const ang = (n / nodes) * Math.PI * 2 + time * 0.8;
            const dist = activeRadius * (0.85 + (n % 2 === 0 ? 0.25 : 0.12) * (1 + reactiveBoost * 0.4));
            const nx = centerX + Math.cos(ang) * dist;
            const ny = centerY + Math.sin(ang) * dist;

            ctx.strokeStyle = `${colors.glow}0.25)`;
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(centerX, centerY);
            ctx.lineTo(nx, ny);
            ctx.stroke();

            ctx.fillStyle = n % 2 === 0 ? '#ffffff' : colors.mid;
            ctx.beginPath();
            ctx.arc(nx, ny, (2.5 + reactiveBoost * 2.5), 0, Math.PI * 2);
            ctx.fill();
          }
          break;
        }
      }

      ctx.restore();
      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [state, settings, audioLevel]);

  return (
    <div
      onClick={onClick}
      className="relative flex items-center justify-center cursor-pointer select-none group"
      title="Click to interact with MYRA"
    >
      <canvas
        ref={canvasRef}
        width={340}
        height={340}
        className="w-[280px] h-[280px] sm:w-[320px] sm:h-[320px] transition-transform duration-300 group-hover:scale-105 active:scale-95"
      />

      {/* State Badge in Center Orbit */}
      <div className="absolute -bottom-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wider uppercase border border-slate-700/80 bg-slate-900/90 backdrop-blur-md shadow-lg flex items-center gap-1.5 text-slate-200">
        <span
          className={`w-2 h-2 rounded-full ${
            state === 'listening'
              ? 'bg-rose-500 animate-ping'
              : state === 'thinking'
              ? 'bg-amber-400 animate-pulse'
              : state === 'speaking'
              ? 'bg-emerald-400 animate-bounce'
              : 'bg-indigo-400'
          }`}
        />
        <span>
          {state === 'idle' && 'Tap Orb to Speak'}
          {state === 'listening' && 'Listening...'}
          {state === 'thinking' && 'Processing...'}
          {state === 'speaking' && 'Speaking...'}
        </span>
      </div>
    </div>
  );
};
