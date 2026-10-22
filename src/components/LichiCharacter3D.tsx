import React, { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import { EmotionState } from '../types';
import { RotateCcw, Sparkles } from 'lucide-react';

interface LichiCharacter3DProps {
  emotion: EmotionState;
  isSpeaking: boolean;
  onTapCharacter?: () => void;
}

const EMOTION_COLORS: Record<EmotionState, { primary: number; secondary: number; name: string }> = {
  happy: { primary: 0xff2a85, secondary: 0xffaa00, name: 'Happy (আনন্দিত)' },
  calm: { primary: 0x00e5ff, secondary: 0x8a2be2, name: 'Calm (শান্ত)' },
  curious: { primary: 0xffb300, secondary: 0x00e5ff, name: 'Curious (কৌতূহলী)' },
  surprised: { primary: 0xba68c8, secondary: 0x00e5ff, name: 'Surprised (বিস্মিত)' },
  empathetic: { primary: 0x00e676, secondary: 0x00b0ff, name: 'Empathetic (সহানুভূতিশীল)' },
};

export const LichiCharacter3D: React.FC<LichiCharacter3DProps> = ({
  emotion,
  isSpeaking,
  onTapCharacter,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null);
  const sceneRef = useRef<THREE.Scene | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);

  // References to animated character parts
  const headGroupRef = useRef<THREE.Group | null>(null);
  const mouthMeshRef = useRef<THREE.Mesh | null>(null);
  const leftEyelidRef = useRef<THREE.Mesh | null>(null);
  const rightEyelidRef = useRef<THREE.Mesh | null>(null);
  const eyeBrowsGroupRef = useRef<THREE.Group | null>(null);
  const particlesRef = useRef<THREE.Points | null>(null);
  const pointLightRef = useRef<THREE.PointLight | null>(null);
  const rimLightRef = useRef<THREE.PointLight | null>(null);
  const hairGlowMeshRef = useRef<THREE.Mesh | null>(null);

  // User rotation drag state
  const isDraggingRef = useRef(false);
  const previousMousePositionRef = useRef({ x: 0, y: 0 });
  const targetRotationRef = useRef({ x: 0, y: 0 });
  const currentRotationRef = useRef({ x: 0, y: 0 });
  const [webGlSupported, setWebGlSupported] = useState(true);

  // Initialize Three.js scene
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    // Check WebGL availability
    try {
      const testCanvas = document.createElement('canvas');
      const gl = testCanvas.getContext('webgl') || testCanvas.getContext('experimental-webgl');
      if (!gl) {
        setWebGlSupported(false);
        return;
      }
    } catch {
      setWebGlSupported(false);
      return;
    }

    const width = container.clientWidth || 360;
    const height = container.clientHeight || 420;

    // 1. Scene
    const scene = new THREE.Scene();
    sceneRef.current = scene;

    // 2. Camera
    const camera = new THREE.PerspectiveCamera(40, width / height, 0.1, 1000);
    camera.position.set(0, 0.4, 4.4);
    cameraRef.current = camera;

    // 3. Renderer with antialiasing & transparent background
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'high-performance' });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    container.appendChild(renderer.domElement);
    rendererRef.current = renderer;

    // 4. Lights
    const ambientLight = new THREE.AmbientLight(0xffffff, 1.2);
    scene.add(ambientLight);

    const dirLight = new THREE.DirectionalLight(0xffffff, 1.8);
    dirLight.position.set(2, 4, 3);
    scene.add(dirLight);

    const pointLight = new THREE.PointLight(0x00e5ff, 2.5, 12);
    pointLight.position.set(0, 1.5, 2.5);
    scene.add(pointLight);
    pointLightRef.current = pointLight;

    const rimLight = new THREE.PointLight(0xff2a85, 3.0, 10);
    rimLight.position.set(-2.5, 1.0, -2.0);
    scene.add(rimLight);
    rimLightRef.current = rimLight;

    // 5. Build 3D Character Model (Lichi)
    const characterGroup = new THREE.Group();
    scene.add(characterGroup);

    // Torso / Shoulders with Cyber Collar
    const bodyGroup = new THREE.Group();
    const torsoGeo = new THREE.CylinderGeometry(0.55, 0.8, 1.2, 32);
    const torsoMat = new THREE.MeshStandardMaterial({
      color: 0x14192b,
      roughness: 0.35,
      metalness: 0.7,
    });
    const torso = new THREE.Mesh(torsoGeo, torsoMat);
    torso.position.y = -1.2;
    bodyGroup.add(torso);

    // Cyber Jacket Accents
    const collarGeo = new THREE.TorusGeometry(0.48, 0.08, 16, 32);
    const collarMat = new THREE.MeshStandardMaterial({
      color: 0x00e5ff,
      emissive: 0x00e5ff,
      emissiveIntensity: 0.6,
      roughness: 0.2,
    });
    const collar = new THREE.Mesh(collarGeo, collarMat);
    collar.rotation.x = Math.PI / 2;
    collar.position.y = -0.55;
    bodyGroup.add(collar);

    // Slender Neck
    const neckGeo = new THREE.CylinderGeometry(0.22, 0.25, 0.6, 24);
    const skinMat = new THREE.MeshStandardMaterial({
      color: 0xffdfd0,
      roughness: 0.55,
      metalness: 0.05,
    });
    const neck = new THREE.Mesh(neckGeo, skinMat);
    neck.position.y = -0.32;
    bodyGroup.add(neck);

    characterGroup.add(bodyGroup);

    // Head Group (rotates dynamically)
    const headGroup = new THREE.Group();
    headGroup.position.y = 0.3;
    characterGroup.add(headGroup);
    headGroupRef.current = headGroup;

    // Stylized Anime Head
    const headGeo = new THREE.SphereGeometry(0.72, 36, 36);
    headGeo.scale(1, 1.15, 0.95);
    const head = new THREE.Mesh(headGeo, skinMat);
    headGroup.add(head);

    // Hair Material (Cyber Violet with subtle sheen)
    const hairMat = new THREE.MeshStandardMaterial({
      color: 0x241838,
      roughness: 0.45,
      metalness: 0.25,
    });

    // Hair back volume
    const hairBackGeo = new THREE.SphereGeometry(0.82, 32, 32);
    hairBackGeo.scale(1.05, 1.15, 1.05);
    const hairBack = new THREE.Mesh(hairBackGeo, hairMat);
    hairBack.position.set(0, 0.1, -0.15);
    headGroup.add(hairBack);

    // Hair bangs
    const bangGeo = new THREE.ConeGeometry(0.32, 0.8, 16);
    const bangLeft = new THREE.Mesh(bangGeo, hairMat);
    bangLeft.rotation.z = 0.5;
    bangLeft.rotation.x = -0.3;
    bangLeft.position.set(-0.35, 0.45, 0.62);
    headGroup.add(bangLeft);

    const bangRight = new THREE.Mesh(bangGeo, hairMat);
    bangRight.rotation.z = -0.5;
    bangRight.rotation.x = -0.3;
    bangRight.position.set(0.35, 0.45, 0.62);
    headGroup.add(bangRight);

    // Glowing Neon Hair Strand
    const neonStrandGeo = new THREE.TorusGeometry(0.75, 0.035, 16, 32, Math.PI * 0.7);
    const neonStrandMat = new THREE.MeshStandardMaterial({
      color: 0x00e5ff,
      emissive: 0x00e5ff,
      emissiveIntensity: 0.9,
    });
    const neonStrand = new THREE.Mesh(neonStrandGeo, neonStrandMat);
    neonStrand.rotation.z = -0.4;
    neonStrand.rotation.y = 0.3;
    neonStrand.position.set(-0.1, 0.3, 0.55);
    headGroup.add(neonStrand);
    hairGlowMeshRef.current = neonStrand;

    // Cybernetic Ear Communicators (Headset)
    const earGeo = new THREE.CylinderGeometry(0.18, 0.18, 0.12, 24);
    const earMat = new THREE.MeshStandardMaterial({
      color: 0x1e253e,
      roughness: 0.2,
      metalness: 0.8,
    });
    const earRingMat = new THREE.MeshStandardMaterial({
      color: 0x00e5ff,
      emissive: 0x00e5ff,
      emissiveIntensity: 1.0,
    });

    const leftEar = new THREE.Mesh(earGeo, earMat);
    leftEar.rotation.z = Math.PI / 2;
    leftEar.position.set(-0.75, 0.05, 0.0);
    headGroup.add(leftEar);

    const leftEarRing = new THREE.Mesh(new THREE.TorusGeometry(0.15, 0.025, 16, 24), earRingMat);
    leftEarRing.rotation.y = Math.PI / 2;
    leftEarRing.position.set(-0.82, 0.05, 0.0);
    headGroup.add(leftEarRing);

    const rightEar = new THREE.Mesh(earGeo, earMat);
    rightEar.rotation.z = Math.PI / 2;
    rightEar.position.set(0.75, 0.05, 0.0);
    headGroup.add(rightEar);

    const rightEarRing = new THREE.Mesh(new THREE.TorusGeometry(0.15, 0.025, 16, 24), earRingMat);
    rightEarRing.rotation.y = Math.PI / 2;
    rightEarRing.position.set(0.82, 0.05, 0.0);
    headGroup.add(rightEarRing);

    // 3D Eyes
    const createEye = (x: number) => {
      const eyeGroup = new THREE.Group();
      eyeGroup.position.set(x, 0.1, 0.6);

      // Sclera (White)
      const eyeWhite = new THREE.Mesh(
        new THREE.SphereGeometry(0.14, 24, 24),
        new THREE.MeshBasicMaterial({ color: 0xffffff })
      );
      eyeGroup.add(eyeWhite);

      // Iris (Vibrant Cyan)
      const iris = new THREE.Mesh(
        new THREE.CircleGeometry(0.095, 24),
        new THREE.MeshBasicMaterial({ color: 0x00e5ff })
      );
      iris.position.z = 0.13;
      eyeGroup.add(iris);

      // Pupil (Deep Dark)
      const pupil = new THREE.Mesh(
        new THREE.CircleGeometry(0.05, 24),
        new THREE.MeshBasicMaterial({ color: 0x0d121f })
      );
      pupil.position.z = 0.135;
      eyeGroup.add(pupil);

      // Glitter Catchlight
      const catchLight = new THREE.Mesh(
        new THREE.CircleGeometry(0.025, 16),
        new THREE.MeshBasicMaterial({ color: 0xffffff })
      );
      catchLight.position.set(0.03, 0.03, 0.138);
      eyeGroup.add(catchLight);

      // Eyelid for Blinking (Rotates to cover eye)
      const eyelidGeo = new THREE.SphereGeometry(0.15, 24, 16, 0, Math.PI * 2, 0, Math.PI * 0.5);
      const eyelid = new THREE.Mesh(eyelidGeo, skinMat);
      eyelid.rotation.x = -Math.PI / 2;
      eyelid.visible = false;
      eyeGroup.add(eyelid);

      return { eyeGroup, eyelid };
    };

    const leftEyeObj = createEye(-0.25);
    const rightEyeObj = createEye(0.25);
    headGroup.add(leftEyeObj.eyeGroup);
    headGroup.add(rightEyeObj.eyeGroup);
    leftEyelidRef.current = leftEyeObj.eyelid;
    rightEyelidRef.current = rightEyeObj.eyelid;

    // Eyebrows
    const eyeBrowsGroup = new THREE.Group();
    const browGeo = new THREE.BoxGeometry(0.22, 0.03, 0.02);
    const browMat = new THREE.MeshBasicMaterial({ color: 0x241838 });

    const leftBrow = new THREE.Mesh(browGeo, browMat);
    leftBrow.position.set(-0.25, 0.32, 0.65);
    leftBrow.rotation.z = 0.08;
    eyeBrowsGroup.add(leftBrow);

    const rightBrow = new THREE.Mesh(browGeo, browMat);
    rightBrow.position.set(0.25, 0.32, 0.65);
    rightBrow.rotation.z = -0.08;
    eyeBrowsGroup.add(rightBrow);

    headGroup.add(eyeBrowsGroup);
    eyeBrowsGroupRef.current = eyeBrowsGroup;

    // Cute Nose
    const noseGeo = new THREE.ConeGeometry(0.04, 0.08, 16);
    const nose = new THREE.Mesh(noseGeo, skinMat);
    nose.rotation.x = 0.4;
    nose.position.set(0, -0.05, 0.72);
    headGroup.add(nose);

    // Expressive 3D Mouth (Animates with visemes and emotions)
    const mouthGeo = new THREE.CylinderGeometry(0.12, 0.12, 0.04, 24);
    const mouthMat = new THREE.MeshBasicMaterial({ color: 0xc25b6c });
    const mouth = new THREE.Mesh(mouthGeo, mouthMat);
    mouth.rotation.x = Math.PI / 2;
    mouth.position.set(0, -0.26, 0.68);
    headGroup.add(mouth);
    mouthMeshRef.current = mouth;

    // 6. Floating Cyber Dust Particles
    const particleCount = 140;
    const particleGeo = new THREE.BufferGeometry();
    const positions = new Float32Array(particleCount * 3);
    for (let i = 0; i < particleCount * 3; i += 3) {
      positions[i] = (Math.random() - 0.5) * 6;
      positions[i + 1] = (Math.random() - 0.5) * 6;
      positions[i + 2] = (Math.random() - 0.5) * 4;
    }
    particleGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    const particleMat = new THREE.PointsMaterial({
      color: 0x00e5ff,
      size: 0.035,
      transparent: true,
      opacity: 0.7,
      blending: THREE.AdditiveBlending,
    });
    const particles = new THREE.Points(particleGeo, particleMat);
    scene.add(particles);
    particlesRef.current = particles;

    // 7. Mouse/Touch Drag Controls
    const onMouseDown = (e: MouseEvent) => {
      isDraggingRef.current = true;
      previousMousePositionRef.current = { x: e.clientX, y: e.clientY };
    };

    const onMouseMove = (e: MouseEvent) => {
      if (!isDraggingRef.current) return;
      const deltaX = e.clientX - previousMousePositionRef.current.x;
      const deltaY = e.clientY - previousMousePositionRef.current.y;

      targetRotationRef.current.y += deltaX * 0.008;
      targetRotationRef.current.x = Math.max(-0.4, Math.min(0.4, targetRotationRef.current.x + deltaY * 0.006));

      previousMousePositionRef.current = { x: e.clientX, y: e.clientY };
    };

    const onMouseUp = () => {
      isDraggingRef.current = false;
    };

    const onTouchStart = (e: TouchEvent) => {
      if (e.touches.length === 1) {
        isDraggingRef.current = true;
        previousMousePositionRef.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      }
    };

    const onTouchMove = (e: TouchEvent) => {
      if (!isDraggingRef.current || e.touches.length !== 1) return;
      const deltaX = e.touches[0].clientX - previousMousePositionRef.current.x;
      const deltaY = e.touches[0].clientY - previousMousePositionRef.current.y;

      targetRotationRef.current.y += deltaX * 0.01;
      targetRotationRef.current.x = Math.max(-0.4, Math.min(0.4, targetRotationRef.current.x + deltaY * 0.008));

      previousMousePositionRef.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
    };

    const onTouchEnd = () => {
      isDraggingRef.current = false;
    };

    const domElement = renderer.domElement;
    domElement.addEventListener('mousedown', onMouseDown);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    domElement.addEventListener('touchstart', onTouchStart, { passive: true });
    window.addEventListener('touchmove', onTouchMove, { passive: true });
    window.addEventListener('touchend', onTouchEnd);

    // 8. Natural Blinking Loop
    let blinkTimer: ReturnType<typeof setInterval>;
    const startBlinking = () => {
      blinkTimer = setInterval(() => {
        if (leftEyelidRef.current && rightEyelidRef.current) {
          leftEyelidRef.current.visible = true;
          rightEyelidRef.current.visible = true;
          setTimeout(() => {
            if (leftEyelidRef.current && rightEyelidRef.current) {
              leftEyelidRef.current.visible = false;
              rightEyelidRef.current.visible = false;
            }
          }, 110);
        }
      }, 3400);
    };
    startBlinking();

    // 9. Main Animation Loop
    let animationFrameId: number;
    let clock = new THREE.Clock();

    const animate = () => {
      animationFrameId = requestAnimationFrame(animate);
      const elapsedTime = clock.getElapsedTime();

      // Smooth dampening towards target rotation
      currentRotationRef.current.y += (targetRotationRef.current.y - currentRotationRef.current.y) * 0.1;
      currentRotationRef.current.x += (targetRotationRef.current.x - currentRotationRef.current.x) * 0.1;

      // Natural Breathing Sway & Idle Movement
      const breathSway = Math.sin(elapsedTime * 1.8) * 0.04;
      const headTilt = Math.sin(elapsedTime * 1.2) * 0.03;

      if (headGroupRef.current) {
        headGroupRef.current.position.y = 0.3 + breathSway;
        headGroupRef.current.rotation.y = currentRotationRef.current.y + headTilt;
        headGroupRef.current.rotation.x = currentRotationRef.current.x;
      }

      if (bodyGroup) {
        bodyGroup.position.y = breathSway * 0.6;
        bodyGroup.rotation.y = currentRotationRef.current.y * 0.4;
      }

      // Rotate background particles
      if (particlesRef.current) {
        particlesRef.current.rotation.y = elapsedTime * 0.05;
        particlesRef.current.rotation.x = Math.sin(elapsedTime * 0.2) * 0.1;
      }

      // Render
      renderer.render(scene, camera);
    };

    animate();

    // 10. Resize handler
    const handleResize = () => {
      if (!container || !renderer || !camera) return;
      const newWidth = container.clientWidth;
      const newHeight = container.clientHeight;
      camera.aspect = newWidth / newHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(newWidth, newHeight);
    };
    window.addEventListener('resize', handleResize);

    return () => {
      clearInterval(blinkTimer);
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('resize', handleResize);
      domElement.removeEventListener('mousedown', onMouseDown);
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
      domElement.removeEventListener('touchstart', onTouchStart);
      window.removeEventListener('touchmove', onTouchMove);
      window.removeEventListener('touchend', onTouchEnd);

      if (container.contains(domElement)) {
        container.removeChild(domElement);
      }
      renderer.dispose();
    };
  }, []);

  // Update Lights and Materials when Emotion Changes
  useEffect(() => {
    const config = EMOTION_COLORS[emotion] || EMOTION_COLORS.calm;

    if (pointLightRef.current) {
      pointLightRef.current.color.setHex(config.primary);
    }
    if (rimLightRef.current) {
      rimLightRef.current.color.setHex(config.secondary);
    }
    if (hairGlowMeshRef.current) {
      const mat = hairGlowMeshRef.current.material as THREE.MeshStandardMaterial;
      mat.color.setHex(config.primary);
      mat.emissive.setHex(config.primary);
    }

    // Adjust eyebrow expression
    if (eyeBrowsGroupRef.current) {
      if (emotion === 'happy') {
        eyeBrowsGroupRef.current.position.y = 0.04;
        eyeBrowsGroupRef.current.rotation.z = 0;
      } else if (emotion === 'curious') {
        eyeBrowsGroupRef.current.position.y = 0.05;
        eyeBrowsGroupRef.current.rotation.z = 0.12;
      } else if (emotion === 'surprised') {
        eyeBrowsGroupRef.current.position.y = 0.08;
      } else if (emotion === 'empathetic') {
        eyeBrowsGroupRef.current.position.y = -0.02;
        eyeBrowsGroupRef.current.rotation.z = -0.06;
      } else {
        eyeBrowsGroupRef.current.position.y = 0;
        eyeBrowsGroupRef.current.rotation.z = 0;
      }
    }
  }, [emotion]);

  // Mouth viseme sync animation when speaking
  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (isSpeaking && mouthMeshRef.current) {
      interval = setInterval(() => {
        if (mouthMeshRef.current) {
          const openScaleY = 0.4 + Math.random() * 2.2;
          const openScaleX = 0.8 + Math.random() * 0.4;
          mouthMeshRef.current.scale.set(openScaleX, openScaleY, 1);
        }
      }, 85);
    } else if (mouthMeshRef.current) {
      mouthMeshRef.current.scale.set(1, emotion === 'happy' ? 0.8 : 0.25, 1);
    }
    return () => clearInterval(interval);
  }, [isSpeaking, emotion]);

  const resetView = () => {
    targetRotationRef.current = { x: 0, y: 0 };
  };

  return (
    <div className="relative w-full h-full flex items-center justify-center select-none overflow-hidden">
      {webGlSupported ? (
        <div
          ref={containerRef}
          onClick={onTapCharacter}
          className="w-full h-full cursor-grab active:cursor-grabbing"
          title="Drag to rotate Lichi in 3D"
        />
      ) : (
        // Graceful 2D Fallback if WebGL is unavailable
        <div className="flex flex-col items-center justify-center p-6 text-center">
          <div className="w-40 h-40 rounded-full bg-gradient-to-tr from-cyan-500 via-purple-600 to-pink-500 flex items-center justify-center shadow-2xl animate-pulse">
            <span className="text-4xl">👧</span>
          </div>
          <p className="mt-4 text-cyan-400 font-semibold text-sm">Lichi 2D Fallback Mode</p>
        </div>
      )}

      {/* Floating 3D Control Hints & Reset Button */}
      <div className="absolute bottom-3 right-3 flex items-center gap-2">
        <button
          onClick={resetView}
          className="p-1.5 rounded-full bg-black/60 hover:bg-black/80 text-cyan-400 border border-cyan-500/30 transition-all backdrop-blur-md"
          title="Reset 3D camera angle"
        >
          <RotateCcw className="w-3.5 h-3.5" />
        </button>
        <div className="px-2.5 py-1 rounded-full bg-black/60 border border-slate-800 text-[10px] text-slate-300 font-medium backdrop-blur-md flex items-center gap-1.5">
          <Sparkles className="w-3 h-3 text-cyan-400" />
          <span>3D Interactive</span>
        </div>
      </div>
    </div>
  );
};
