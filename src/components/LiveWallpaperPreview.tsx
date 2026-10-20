/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef, useState } from 'react';
import { WallpaperSettings } from '../types/wallpaper';

interface LiveWallpaperPreviewProps {
  settings: WallpaperSettings;
  className?: string;
  onTap?: () => void;
}

interface Star {
  x: number;
  y: number;
  speed: number;
  size: number;
  alpha: number;
}

interface Particle {
  x: number;
  y: number;
  vy: number;
  size: number;
  alpha: number;
  colorType: number; // 0: blue, 1: red
}

const APP_ICONS = [
  { name: 'Phone', glyph: '📞', angleDeg: 0, color: '#22c55e' },
  { name: 'WhatsApp', glyph: '💬', angleDeg: 32.7, color: '#25d366' },
  { name: 'Chrome', glyph: '🌐', angleDeg: 65.4, color: '#3b82f6' },
  { name: 'YouTube', glyph: '▶', angleDeg: 98.1, color: '#ef4444' },
  { name: 'Instagram', glyph: '📷', angleDeg: 130.8, color: '#ec4899' },
  { name: 'Messages', glyph: '✉', angleDeg: 163.5, color: '#3b82f6' },
  { name: 'Camera', glyph: '📸', angleDeg: 196.2, color: '#f59e0b' },
  { name: 'Photos', glyph: '🖼', angleDeg: 228.9, color: '#a855f7' },
  { name: 'Play Store', glyph: '🛒', angleDeg: 261.6, color: '#10b981' },
  { name: 'Gallery', glyph: '🌌', angleDeg: 294.3, color: '#0ea5e9' },
  { name: 'Settings', glyph: '⚙', angleDeg: 327, color: '#94a3b8' },
];

export const LiveWallpaperPreview: React.FC<LiveWallpaperPreviewProps> = ({
  settings,
  className = '',
  onTap,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animFrameRef = useRef<number | null>(null);

  const starsRef = useRef<Star[]>([]);
  const particlesRef = useRef<Particle[]>([]);
  const animTimeRef = useRef<number>(0);
  const lastTimeRef = useRef<number>(performance.now());
  const rippleRef = useRef<{ x: number; y: number; radius: number; alpha: number } | null>(null);

  // Speed multiplier
  const speedMult = settings.speed === 'slow' ? 0.6 : settings.speed === 'fast' ? 1.5 : 1.0;

  // Initialize stars and particles
  const initObjects = (width: number, height: number) => {
    const starCount = settings.batterySaver ? 35 : 75;
    starsRef.current = Array.from({ length: starCount }, () => ({
      x: Math.random() * width,
      y: Math.random() * height,
      speed: 10 + Math.random() * 25,
      size: 1 + Math.random() * 2.2,
      alpha: 0.3 + Math.random() * 0.7,
    }));

    const particleCount = settings.batterySaver ? 15 : 40;
    particlesRef.current = Array.from({ length: particleCount }, () => ({
      x: Math.random() * width,
      y: Math.random() * height,
      vy: -(15 + Math.random() * 35),
      size: 2 + Math.random() * 3.5,
      alpha: 0.2 + Math.random() * 0.8,
      colorType: Math.random() > 0.5 ? 0 : 1,
    }));
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const width = canvas.width || 360;
    const height = canvas.height || 640;
    initObjects(width, height);

    let isRunning = true;
    lastTimeRef.current = performance.now();

    const render = (time: number) => {
      if (!isRunning) return;

      const dt = Math.min((time - lastTimeRef.current) / 1000, 0.1);
      lastTimeRef.current = time;

      animTimeRef.current += dt * speedMult;
      const t = animTimeRef.current;

      const ctx = canvas.getContext('2d');
      if (ctx) {
        const w = canvas.width;
        const h = canvas.height;
        const cx = w / 2;
        const cy = h / 2;

        // 1. Deep Space Futuristic Background
        const grad = ctx.createLinearGradient(0, 0, 0, h);
        grad.addColorStop(0, '#040712');
        grad.addColorStop(0.4, '#080e22');
        grad.addColorStop(0.8, '#130514');
        grad.addColorStop(1, '#03050d');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, w, h);

        // 2. Stars
        for (const star of starsRef.current) {
          star.y += star.speed * dt * speedMult;
          if (star.y > h) {
            star.y = 0;
            star.x = Math.random() * w;
          }
          ctx.beginPath();
          ctx.arc(star.x, star.y, star.size, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(255, 255, 255, ${star.alpha})`;
          ctx.fill();
        }

        // 3. Perspective Digital Grid at Bottom
        const gridYStart = h * 0.72;
        const vpX = w / 2;
        const vpY = h * 0.65;
        const gridColor =
          settings.theme === 'blue'
            ? 'rgba(0, 180, 255, 0.18)'
            : settings.theme === 'red'
            ? 'rgba(255, 40, 80, 0.18)'
            : 'rgba(0, 200, 255, 0.18)';

        ctx.strokeStyle = gridColor;
        ctx.lineWidth = 1;
        for (let i = -7; i <= 7; i++) {
          const bottomX = vpX + i * (w * 0.14);
          ctx.beginPath();
          ctx.moveTo(vpX, vpY + 40);
          ctx.lineTo(bottomX, h);
          ctx.stroke();
        }

        const offset = (t * 30) % 35;
        for (let y = gridYStart + offset; y < h; y += 28) {
          const progress = (y - gridYStart) / (h - gridYStart);
          ctx.strokeStyle =
            settings.theme === 'red'
              ? `rgba(255, 50, 90, ${progress * 0.25})`
              : `rgba(0, 220, 255, ${progress * 0.25})`;
          ctx.beginPath();
          ctx.moveTo(0, y);
          ctx.lineTo(w, y);
          ctx.stroke();
        }

        // 4. Holographic Earth (Center Upper)
        if (settings.showEarth) {
          const earthRadius = Math.min(w, h) * 0.23;
          const earthX = cx;
          const earthY = h * 0.30;
          const earthAngle = t * 0.15;

          // Outer atmospheric glow
          const atmGrad = ctx.createRadialGradient(earthX, earthY, earthRadius * 0.7, earthX, earthY, earthRadius * 1.35);
          atmGrad.addColorStop(0, 'rgba(0, 160, 255, 0.35)');
          atmGrad.addColorStop(0.7, 'rgba(0, 100, 220, 0.12)');
          atmGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');
          ctx.fillStyle = atmGrad;
          ctx.beginPath();
          ctx.arc(earthX, earthY, earthRadius * 1.35, 0, Math.PI * 2);
          ctx.fill();

          // Globe Body
          const globeGrad = ctx.createRadialGradient(
            earthX - earthRadius * 0.3,
            earthY - earthRadius * 0.3,
            earthRadius * 0.1,
            earthX,
            earthY,
            earthRadius
          );
          globeGrad.addColorStop(0, '#0c2e56');
          globeGrad.addColorStop(0.6, '#061730');
          globeGrad.addColorStop(1, '#020914');
          ctx.fillStyle = globeGrad;
          ctx.beginPath();
          ctx.arc(earthX, earthY, earthRadius, 0, Math.PI * 2);
          ctx.fill();

          // Holographic Latitudes
          ctx.strokeStyle = 'rgba(0, 230, 255, 0.32)';
          ctx.lineWidth = 1.2;
          for (let i = -3; i <= 3; i++) {
            const latY = earthY + i * (earthRadius * 0.26);
            const dist = Math.abs(latY - earthY);
            if (dist < earthRadius) {
              const chordHalf = Math.sqrt(earthRadius * earthRadius - dist * dist);
              ctx.beginPath();
              ctx.ellipse(earthX, latY, chordHalf, 5, 0, 0, Math.PI * 2);
              ctx.stroke();
            }
          }

          // Rotating Longitudes
          for (let i = 0; i < 5; i++) {
            const lonAngle = earthAngle + (i * Math.PI) / 5;
            const aspect = Math.cos(lonAngle);
            ctx.strokeStyle = `rgba(0, 230, 255, ${0.15 + Math.abs(aspect) * 0.25})`;
            ctx.beginPath();
            ctx.ellipse(earthX, earthY, earthRadius * Math.abs(aspect), earthRadius, 0, 0, Math.PI * 2);
            ctx.stroke();
          }

          // Thin rim halo
          ctx.strokeStyle = 'rgba(0, 240, 255, 0.65)';
          ctx.lineWidth = 1.8;
          ctx.beginPath();
          ctx.arc(earthX, earthY, earthRadius, 0, Math.PI * 2);
          ctx.stroke();
        }

        // 5. MYRA AI Female Avatar Silhouette & Visor
        const avatarSize = Math.min(w, h) * 0.21;
        const avX = cx;
        const avY = h * 0.32;

        ctx.save();
        ctx.fillStyle = 'rgba(6, 28, 55, 0.45)';
        ctx.strokeStyle =
          settings.theme === 'red'
            ? 'rgba(255, 60, 90, 0.85)'
            : 'rgba(0, 230, 255, 0.85)';
        ctx.lineWidth = 1.8;

        // Head and shoulders path
        ctx.beginPath();
        ctx.arc(avX, avY - avatarSize * 0.35, avatarSize * 0.22, 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();

        ctx.beginPath();
        ctx.moveTo(avX - avatarSize * 0.1, avY - avatarSize * 0.14);
        ctx.lineTo(avX + avatarSize * 0.1, avY - avatarSize * 0.14);
        ctx.lineTo(avX + avatarSize * 0.26, avY + avatarSize * 0.16);
        ctx.lineTo(avX + avatarSize * 0.44, avY + avatarSize * 0.44);
        ctx.lineTo(avX - avatarSize * 0.44, avY + avatarSize * 0.44);
        ctx.lineTo(avX - avatarSize * 0.26, avY + avatarSize * 0.16);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        // Neon Visor / Eye band
        ctx.fillStyle = settings.theme === 'red' ? '#ff2a55' : '#00ffff';
        ctx.shadowColor = settings.theme === 'red' ? '#ff2a55' : '#00ffff';
        ctx.shadowBlur = 10;
        ctx.beginPath();
        ctx.roundRect(avX - avatarSize * 0.12, avY - avatarSize * 0.38, avatarSize * 0.24, 5, 2);
        ctx.fill();
        ctx.shadowBlur = 0;

        // Cyber crown arc
        ctx.strokeStyle = 'rgba(255, 50, 100, 0.7)';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(avX, avY - avatarSize * 0.52, avatarSize * 0.3, Math.PI * 1.1, Math.PI * 1.9);
        ctx.stroke();
        ctx.restore();

        // 6. Holographic HUD Rings (Centered around Orb Area)
        const orbCenterY = h * 0.56;
        const maxRingRadius = Math.min(w, h) * 0.38;

        const ring1R = maxRingRadius * 0.65;
        const ring2R = maxRingRadius * 0.85;
        const ring3R = maxRingRadius * 1.05;

        // Segmented Inner Ring (Clockwise)
        ctx.strokeStyle =
          settings.theme === 'red'
            ? 'rgba(255, 60, 90, 0.6)'
            : 'rgba(0, 220, 255, 0.6)';
        ctx.lineWidth = 2.2;
        const r1Angle = (t * 0.4) % (Math.PI * 2);
        for (let i = 0; i < 4; i++) {
          const startA = r1Angle + (i * Math.PI) / 2;
          ctx.beginPath();
          ctx.arc(cx, orbCenterY, ring1R, startA, startA + 0.9);
          ctx.stroke();
        }

        // Middle Ring (Counter-Clockwise Cyber Red/Blue)
        ctx.strokeStyle =
          settings.theme === 'blue'
            ? 'rgba(0, 180, 255, 0.6)'
            : 'rgba(255, 40, 80, 0.7)';
        ctx.lineWidth = 2.0;
        const r2Angle = (-t * 0.3) % (Math.PI * 2);
        ctx.beginPath();
        ctx.arc(cx, orbCenterY, ring2R, r2Angle, r2Angle + 1.2);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(cx, orbCenterY, ring2R, r2Angle + 2.2, r2Angle + 3.8);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(cx, orbCenterY, ring2R, r2Angle + 4.5, r2Angle + 5.6);
        ctx.stroke();

        // Traveling Neon Pulse Dot along Ring 2
        const pulseAngle = r2Angle + ((t * 0.8) % 1) * Math.PI * 2;
        const px = cx + ring2R * Math.cos(pulseAngle);
        const py = orbCenterY + ring2R * Math.sin(pulseAngle);
        ctx.fillStyle = '#ffffff';
        ctx.shadowColor = settings.theme === 'blue' ? '#00e5ff' : '#ff2a55';
        ctx.shadowBlur = 12;
        ctx.beginPath();
        ctx.arc(px, py, 4.5, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;

        // Outer Ring with Tick Marks
        ctx.strokeStyle = 'rgba(0, 220, 255, 0.4)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.arc(cx, orbCenterY, ring3R, 0, Math.PI * 2);
        ctx.stroke();

        const tickCount = 24;
        const tickLen = 7;
        ctx.strokeStyle = 'rgba(0, 240, 255, 0.55)';
        ctx.lineWidth = 1.5;
        for (let i = 0; i < tickCount; i++) {
          const a = (i * (Math.PI * 2)) / tickCount + t * 0.1;
          const x1 = cx + (ring3R - tickLen) * Math.cos(a);
          const y1 = orbCenterY + (ring3R - tickLen) * Math.sin(a);
          const x2 = cx + ring3R * Math.cos(a);
          const y2 = orbCenterY + ring3R * Math.sin(a);
          ctx.beginPath();
          ctx.moveTo(x1, y1);
          ctx.lineTo(x2, y2);
          ctx.stroke();
        }

        // 7. Radial App Icons
        if (settings.showAppIcons) {
          const orbitR = Math.min(w, h) * 0.36;
          const baseOrbAngle = t * 0.08;
          const iconSize = Math.max(34, w * 0.085);

          ctx.font = `${Math.round(iconSize * 0.5)}px sans-serif`;
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';

          APP_ICONS.forEach((item, idx) => {
            const rad = baseOrbAngle + (item.angleDeg * Math.PI) / 180;
            const ix = cx + orbitR * 1.08 * Math.cos(rad);
            const iy = orbCenterY + orbitR * 0.82 * Math.sin(rad);
            const floatOffset = Math.sin(t * 2.5 + idx) * 3.5;

            // Icon circle background
            ctx.fillStyle = 'rgba(12, 18, 36, 0.82)';
            ctx.beginPath();
            ctx.arc(ix, iy + floatOffset, iconSize * 0.5, 0, Math.PI * 2);
            ctx.fill();

            // Neon glowing ring
            ctx.strokeStyle = item.color;
            ctx.lineWidth = 2.2;
            ctx.shadowColor = item.color;
            ctx.shadowBlur = 6;
            ctx.beginPath();
            ctx.arc(ix, iy + floatOffset, iconSize * 0.5, 0, Math.PI * 2);
            ctx.stroke();
            ctx.shadowBlur = 0;

            // Glyph
            ctx.fillStyle = '#ffffff';
            ctx.fillText(item.glyph, ix, iy + floatOffset);
          });
        }

        // 8. MYRA Glowing AI Orb (Breathing Core)
        if (settings.showOrb) {
          const breathe = Math.sin(t * 2.2) * 0.08;
          const orbR = Math.min(w, h) * 0.11 * (1 + breathe);

          // Outer Glow
          const outerGlow = ctx.createRadialGradient(cx, orbCenterY, orbR * 0.5, cx, orbCenterY, orbR * 2.2);
          if (settings.theme === 'red') {
            outerGlow.addColorStop(0, 'rgba(255, 40, 80, 0.65)');
            outerGlow.addColorStop(0.6, 'rgba(180, 20, 50, 0.25)');
          } else if (settings.theme === 'blue') {
            outerGlow.addColorStop(0, 'rgba(0, 210, 255, 0.65)');
            outerGlow.addColorStop(0.6, 'rgba(0, 90, 200, 0.25)');
          } else {
            outerGlow.addColorStop(0, 'rgba(0, 230, 255, 0.7)');
            outerGlow.addColorStop(0.6, 'rgba(255, 40, 90, 0.3)');
          }
          outerGlow.addColorStop(1, 'rgba(0, 0, 0, 0)');

          ctx.fillStyle = outerGlow;
          ctx.beginPath();
          ctx.arc(cx, orbCenterY, orbR * 2.2, 0, Math.PI * 2);
          ctx.fill();

          // Core Sphere
          const coreGrad = ctx.createRadialGradient(
            cx - orbR * 0.3,
            orbCenterY - orbR * 0.3,
            orbR * 0.1,
            cx,
            orbCenterY,
            orbR
          );
          coreGrad.addColorStop(0, '#ffffff');
          if (settings.theme === 'red') {
            coreGrad.addColorStop(0.45, '#ff3366');
            coreGrad.addColorStop(1, '#800020');
          } else if (settings.theme === 'blue') {
            coreGrad.addColorStop(0.45, '#00d4ff');
            coreGrad.addColorStop(1, '#0a2560');
          } else {
            coreGrad.addColorStop(0.4, '#00f0ff');
            coreGrad.addColorStop(0.8, '#ff2a6d');
            coreGrad.addColorStop(1, '#4a0033');
          }

          ctx.fillStyle = coreGrad;
          ctx.beginPath();
          ctx.arc(cx, orbCenterY, orbR, 0, Math.PI * 2);
          ctx.fill();

          // Cyber ring revolving around orb
          ctx.save();
          ctx.translate(cx, orbCenterY);
          ctx.rotate(Math.sin(t * 1.5) * 0.3);
          ctx.strokeStyle = 'rgba(255, 255, 255, 0.75)';
          ctx.lineWidth = 2.2;
          ctx.beginPath();
          ctx.ellipse(0, 0, orbR * 1.35, orbR * 0.45, 0, 0, Math.PI * 2);
          ctx.stroke();
          ctx.restore();

          // Subtitle
          ctx.font = 'bold 12px sans-serif';
          ctx.fillStyle = 'rgba(220, 240, 255, 0.9)';
          ctx.textAlign = 'center';
          ctx.fillText('MYRA AI', cx, orbCenterY + orbR * 1.7);
        }

        // 9. Bottom Holographic Energy Platform / Portal
        const portalRadiusX = w * 0.44;
        const portalRadiusY = portalRadiusX * 0.28;
        const portalY = h * 0.90;

        const portGrad = ctx.createRadialGradient(cx, portalY, 10, cx, portalY, portalRadiusX);
        portGrad.addColorStop(0, 'rgba(0, 220, 255, 0.55)');
        portGrad.addColorStop(0.5, 'rgba(255, 40, 90, 0.28)');
        portGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');
        ctx.fillStyle = portGrad;
        ctx.beginPath();
        ctx.ellipse(cx, portalY, portalRadiusX, portalRadiusY, 0, 0, Math.PI * 2);
        ctx.fill();

        ctx.strokeStyle = 'rgba(0, 240, 255, 0.65)';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.ellipse(cx, portalY, portalRadiusX * 0.8, portalRadiusY * 0.8, 0, 0, Math.PI * 2);
        ctx.stroke();

        ctx.strokeStyle = 'rgba(255, 50, 90, 0.65)';
        ctx.beginPath();
        ctx.ellipse(cx, portalY, portalRadiusX * 0.5, portalRadiusY * 0.5, 0, 0, Math.PI * 2);
        ctx.stroke();

        // Vertical light rays rising from portal
        for (let i = -4; i <= 4; i++) {
          const rx = cx + i * (portalRadiusX * 0.2);
          const rayH = 45 + Math.sin(t * 3 + i) * 25;
          const rayGrad = ctx.createLinearGradient(rx, portalY, rx, portalY - rayH);
          rayGrad.addColorStop(0, 'rgba(0, 240, 255, 0.45)');
          rayGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');
          ctx.strokeStyle = rayGrad;
          ctx.lineWidth = 1.8;
          ctx.beginPath();
          ctx.moveTo(rx, portalY);
          ctx.lineTo(rx, portalY - rayH);
          ctx.stroke();
        }

        // 10. Upward Floating Particles
        if (settings.particleEffects) {
          for (const p of particlesRef.current) {
            p.y += p.vy * dt * speedMult;
            if (p.y < 0) {
              p.y = h;
              p.x = Math.random() * w;
              p.alpha = 0.8;
            }
            p.alpha -= 0.05 * dt;
            if (p.alpha < 0.1) p.alpha = 0.8;

            ctx.fillStyle =
              p.colorType === 0
                ? `rgba(0, 230, 255, ${p.alpha})`
                : `rgba(255, 50, 90, ${p.alpha})`;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fill();
          }
        }

        // 11. Touch Ripple
        if (rippleRef.current && rippleRef.current.alpha > 0) {
          const rip = rippleRef.current;
          rip.radius += 260 * dt;
          rip.alpha -= 1.8 * dt;
          ctx.strokeStyle =
            settings.theme === 'red'
              ? `rgba(255, 50, 90, ${rip.alpha})`
              : `rgba(0, 230, 255, ${rip.alpha})`;
          ctx.lineWidth = 3;
          ctx.beginPath();
          ctx.arc(rip.x, rip.y, rip.radius, 0, Math.PI * 2);
          ctx.stroke();
          if (rip.alpha <= 0) rippleRef.current = null;
        }
      }

      animFrameRef.current = requestAnimationFrame(render);
    };

    animFrameRef.current = requestAnimationFrame(render);

    return () => {
      isRunning = false;
      if (animFrameRef.current) {
        cancelAnimationFrame(animFrameRef.current);
      }
    };
  }, [settings, speedMult]);

  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * canvas.width;
    const y = ((e.clientY - rect.top) / rect.height) * canvas.height;
    rippleRef.current = { x, y, radius: 10, alpha: 1.0 };
    onTap?.();
  };

  return (
    <div className={`relative aspect-[9/16] overflow-hidden rounded-2xl shadow-2xl border border-blue-500/30 bg-black ${className}`}>
      <canvas
        ref={canvasRef}
        width={360}
        height={640}
        onClick={handleCanvasClick}
        className="w-full h-full block cursor-pointer select-none object-cover"
      />
    </div>
  );
};
