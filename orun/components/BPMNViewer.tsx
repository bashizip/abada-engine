import React, { useEffect, useRef } from 'react';
// @ts-ignore
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';

interface BPMNViewerProps {
  xml: string;
  activeActivityIds?: string[];
}

export const BPMNViewer: React.FC<BPMNViewerProps> = ({ xml, activeActivityIds = [] }) => {
  const containerRef = useRef<HTMLDivElement>(null);

  if (!xml) return <div className="w-full h-full flex items-center justify-center text-slate-400">No diagram available</div>;

  useEffect(() => {
    if (!containerRef.current) return;

    const viewer = new BpmnViewer({
      container: containerRef.current
    });

    console.log('Importing XML into BPMN Viewer...');
    viewer.importXML(xml).then(() => {
      console.log('XML Imported successfully');
      const canvas = viewer.get('canvas') as any;

      // Zoom to fit
      canvas.zoom('fit-viewport');

      // Highlight active activities
      console.log('Highlighting activities:', activeActivityIds);
      activeActivityIds.forEach(id => {
        try {
          canvas.addMarker(id, 'highlight-active');
          canvas.addMarker(id, 'highlight-pulse');
        } catch (e) {
          console.warn(`Failed to highlight activity ${id}`, e);
        }
      });

    }).catch((err: any) => {
      console.error('Failed to render BPMN', err);
    });

    return () => {
      viewer.destroy();
    };
  }, [xml, activeActivityIds]);

  return (
    <div className="w-full h-full bg-slate-950 rounded-lg overflow-hidden relative border border-slate-800">
      <div ref={containerRef} className="w-full h-full" />
      <style>{`
                /* Dark Theme for BPMN */
                .djs-container svg {
                    background-color: #020617; /* slate-950 */
                }

                /* Shapes (Tasks, Events, Gateways) */
                .djs-visual > :nth-child(1) {
                    stroke: #94a3b8 !important; /* slate-400 */
                    fill: #1e293b !important; /* slate-800 */
                    stroke-width: 1px !important;
                }

                /* Text Labels */
                .djs-visual text, .djs-label {
                    fill: #cbd5e1 !important; /* slate-300 */
                    font-weight: normal !important;
                    font-family: 'Inter', sans-serif !important;
                }

                /* Connections (Arrows) */
                .djs-connection path {
                    stroke: #64748b !important; /* slate-500 */
                    stroke-width: 1px !important;
                }

                /* Arrowheads */
                .djs-marker path {
                    fill: #64748b !important;
                    stroke: #64748b !important;
                }

                /* Inner Symbols (Gateway markers, Event icons, Task icons) */
                .djs-visual > :nth-child(n+2) {
                    stroke: #cbd5e1 !important; /* slate-300 */
                }

                /* Highlighted Active Element (Yellow/Orange Glow) */
                .highlight-active .djs-visual > :nth-child(1) {
                    stroke: #fbbf24 !important; /* amber-400 */
                    fill: #fbbf24 !important;
                    fill-opacity: 0.2 !important;
                    stroke-width: 1px !important;
                    filter: drop-shadow(0 0 12px rgba(251, 191, 36, 0.6));
                }

                .highlight-active text {
                    fill: #fbbf24 !important;
                }
                
                /* Pulse Animation */
                .highlight-pulse .djs-visual > :nth-child(1) {
                    animation: pulse-border 2s infinite !important;
                }

                @keyframes pulse-border {
                    0% { 
                        stroke-opacity: 1; 
                        filter: drop-shadow(0 0 8px rgba(251, 191, 36, 0.6));
                    }
                    50% { 
                        stroke-opacity: 0.4; 
                        filter: drop-shadow(0 0 16px rgba(251, 191, 36, 0.9));
                    }
                    100% { 
                        stroke-opacity: 1; 
                        filter: drop-shadow(0 0 8px rgba(251, 191, 36, 0.6));
                    }
                }

                /* Hide Watermark */
                .bjs-powered-by { display: none; }
                
                /* Hide Palette if present */
                .djs-palette { display: none; }
            `}</style>
    </div>
  );
};
