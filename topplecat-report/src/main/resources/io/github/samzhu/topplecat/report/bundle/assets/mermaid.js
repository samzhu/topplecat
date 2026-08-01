/* ToppleCat's pinned, report-owned offline Mermaid subset renderer. */
(() => {
  const escape = value => String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character]);
  const label = (source, id) => {
    const match = source.match(new RegExp(`${id}\\s*\\[([^\\]]+)]`));
    return match ? match[1].trim() : id;
  };
  window.ToppleCatMermaid = {
    render(source) {
      const lines = String(source ?? '').split(/\r?\n/).map(line => line.trim()).filter(Boolean);
      if (!/^(graph|flowchart)\s+(TD|TB|LR|RL)$/i.test(lines[0] || '')) throw new Error('unsupported diagram declaration');
      const edges = lines.slice(1).map(line => line.match(/^([A-Za-z][\w-]*)\s*(?:\[[^\]]+])?\s*--?>\s*([A-Za-z][\w-]*)(?:\s*\[[^\]]+])?$/)).filter(Boolean);
      if (!edges.length) throw new Error('no safe flow edges');
      const ids = [...new Set(edges.flatMap(edge => [edge[1], edge[2]]))];
      if (ids.length > 40) throw new Error('diagram is too large');
      const horizontal = /\s(LR|RL)$/i.test(lines[0]);
      const width = horizontal ? Math.max(360, ids.length * 160) : 420;
      const height = horizontal ? 180 : Math.max(160, ids.length * 82);
      const point = index => horizontal ? { x: 75 + index * 150, y: 90 } : { x: 210, y: 48 + index * 76 };
      const index = new Map(ids.map((id, position) => [id, position]));
      const svgEdges = edges.map(edge => { const a = point(index.get(edge[1])); const b = point(index.get(edge[2])); return `<line x1="${a.x}" y1="${a.y}" x2="${b.x}" y2="${b.y}" stroke="currentColor" stroke-width="2" marker-end="url(#arrow)"/>`; }).join('');
      const nodes = ids.map((id, position) => { const p = point(position); return `<g><rect x="${p.x - 58}" y="${p.y - 21}" width="116" height="42" rx="6" fill="currentColor" fill-opacity=".08" stroke="currentColor"/><text x="${p.x}" y="${p.y + 5}" text-anchor="middle" fill="currentColor" font-family="system-ui, sans-serif" font-size="13">${escape(label(source, id))}</text></g>`; }).join('');
      return `<svg role="img" aria-label="Mermaid diagram" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg"><defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto"><path d="M0,0 L0,6 L6,3 z" fill="currentColor"/></marker></defs>${svgEdges}${nodes}</svg>`;
    }
  };
})();
