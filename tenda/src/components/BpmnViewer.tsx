import { useEffect, useRef } from 'react';
import BpmnJS from 'bpmn-js/lib/NavigatedViewer';

// Import the necessary CSS for the viewer and the BPMN font
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';

interface BpmnViewerProps {
  xml: string;
}

export function BpmnViewer({ xml }: BpmnViewerProps) {
  const viewerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!viewerRef.current) return;

    const viewer = new BpmnJS({
      container: viewerRef.current,
      keyboard: {
        bindTo: window
      }
    });

    const importXml = async () => {
      try {
        await viewer.importXML(xml);
        const canvas = viewer.get('canvas') as any;
        canvas.zoom('fit-viewport');
      } catch (err) {
        console.error('Failed to import BPMN XML', err);
      }
    };

    importXml();

    return () => {
      viewer.destroy();
    };
  }, [xml]);

  return (
    <div
      ref={viewerRef}
      className="h-full w-full bg-background/50 rounded-lg border border-border overflow-hidden bpmn-container"
    />
  );
}
