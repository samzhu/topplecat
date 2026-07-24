(() => {
  const data = JSON.parse(document.getElementById('topplecat-report-data').textContent);
  const projection = data.schemaVersion.includes('review') ? 'review' : data.schemaVersion.includes('verification') ? 'verification' : 'spec';
  const e = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const pretty = value => e(JSON.stringify(value, null, 2));
  const status = value => `<span class="badge ${e(value)}">${e(value)}</span>`;
  const markdown = blocks => (blocks || []).map(block => block.kind === 'LIST' ? `<ul>${(block.items || []).map(item => `<li>${e(item)}</li>`).join('')}</ul>` : `<p>${e(block.text)}</p>`).join('');
  const scenario = steps => steps?.length ? `<section class="section"><h3>Scenario</h3><ol class="scenario">${steps.map(step => `<li>${e(step)}</li>`).join('')}</ol></section>` : '';
  const attachmentPath = item => /^attachments\/[a-f0-9]{64}\.(png|jpg|json|txt)$/.test(item?.relativePath || '') ? item.relativePath : '';
  const attachments = rows => rows?.length ? `<div class="attachments">${rows.map(item => { const rawPath = attachmentPath(item); if (!rawPath) return ''; const path = e(rawPath); const label = e(item.title); return item.mediaType?.startsWith('image/') ? `<a href="${path}" target="_blank" rel="noopener"><img src="${path}" alt="${label}"><span>${label}</span></a>` : `<a href="${path}" target="_blank" rel="noopener">${label} (${e(item.mediaType)})</a>`; }).join('')}</div>` : '';
  const source = (step, sources) => { const ref = sources?.[step.stepId]; return ref ? `<p class="source-ref">Source: ${e(ref.file)}:${e(ref.line)}:${e(ref.column)}</p>` : ''; };
  const steps = (rows, sources) => rows?.length ? `<ol class="steps">${rows.map(step => `<li class="${e(step.status)}">${e(step.sentence)} ${status(step.status)} ${step.durationNanos ? `<span class="duration">${(step.durationNanos / 1e6).toFixed(1)} ms</span>` : ''}${source(step, sources)}${step.actualArguments?.length ? `<pre class="json">${pretty(step.actualArguments)}</pre>` : ''}${step.failureRef ? `<pre class="failure">${e(step.failureRef)}</pre>` : ''}${attachments(step.attachments)}</li>`).join('')}</ol>` : '';
  const caseMatrix = (row, markers = '') => `<article class="case" tabindex="0"><h3>${e(row.caseId)} ${markers}</h3><div class="case-grid"><div><p class="meta">Inputs</p><pre class="json">${pretty(row.inputs)}</pre></div><div><p class="meta">Expected</p><pre class="json">${pretty(row.expected)}</pre></div></div></article>`;
  const renderSpecCases = ac => (ac.publicCases || []).map(row => caseMatrix(row)).join('');
  const renderReviewCases = ac => (ac.cases || []).map(row => caseMatrix(row, `<span class="badge">${e(row.visibility)}</span>`)).join('');
  const renderVerificationCases = ac => (ac.cases || []).map(row => `<article class="case" tabindex="0"><h3>${e(row.caseId)} <span class="badge">${e(row.visibility)}</span> ${status(row.status)}</h3><div class="case-grid"><div><p class="meta">Inputs</p><pre class="json">${pretty(row.inputs)}</pre></div><div><p class="meta">Expected</p><pre class="json">${pretty(row.expected)}</pre></div></div><div class="section"><p class="meta">Expected consumption</p><pre class="json">${pretty(row.expectedConsumption)}</pre></div>${row.steps.length ? `<div class="section"><h4>Step evidence</h4>${steps(row.steps, ac.stepSources)}</div>` : ''}${row.failure ? `<pre class="failure">${e(row.failure)}</pre>` : ''}</article>`).join('');
  const caseRows = ac => projection === 'spec' ? (ac.publicCases || []) : (ac.cases || []);
  const renderCases = ac => projection === 'spec' ? renderSpecCases(ac) : projection === 'review' ? renderReviewCases(ac) : renderVerificationCases(ac);
  const scenarioFor = ac => projection === 'review' ? (ac.method?.staticSentences || []) : (ac.scenario || []);
  const title = projection === 'review' ? 'Reviewer review' : projection === 'verification' ? 'Verification evidence' : 'Public specification';
  document.getElementById('title').textContent = title;
  document.getElementById('notice').textContent = projection === 'review' ? 'Reviewer-only contract preview. No execution verdict is shown.' : projection === 'verification' ? `Suite verdict: ${data.verdict}` : 'Public contract projection. It contains no reviewer data or execution failure.';
  const acs = data.acceptanceConditions || [];
  const filters = document.getElementById('filters');
  const enabledStatuses = new Set();
  const statuses = ['PASS', 'FAIL', 'NOT_REPORTED'];
  if (projection === 'verification') {
    filters.innerHTML = statuses.map(value => `<button type="button" data-status="${value}" aria-pressed="false">${value}</button>`).join('');
    const toggleStatus = value => {
      if (!value) return;
      enabledStatuses.has(value) ? enabledStatuses.delete(value) : enabledStatuses.add(value);
      filters.querySelectorAll('button').forEach(button => {
        const selected = enabledStatuses.has(button.dataset.status);
        button.setAttribute('aria-pressed', String(selected));
        button.classList.toggle('selected', selected);
      });
      render();
    };
    filters.addEventListener('click', event => {
      toggleStatus(event.target?.dataset?.status);
    });
    filters.addEventListener('keydown', event => {
      if (event.key !== 'Enter' && event.key !== ' ') return;
      const value = event.target?.dataset?.status;
      if (!value) return;
      event.preventDefault();
      toggleStatus(value);
    });
  }
  const gateNotices = (data.gates || []).filter(gate => gate.verdict !== 'PASS').map(gate =>
    `<p class="gate-notice ${e(gate.verdict)}">${e(gate.name)}: ${status(gate.verdict)} ${e(gate.reason || '')}</p>`).join('');
  const render = () => {
    const query = document.getElementById('query').value.trim().toLowerCase();
    const visible = acs.filter(ac => {
      const rows = caseRows(ac);
      const textMatches = !query || `${ac.acId} ${ac.title} ${rows.map(c => c.caseId).join(' ')}`.toLowerCase().includes(query);
      const statusMatches = !enabledStatuses.size || enabledStatuses.has(ac.status) || rows.some(row => enabledStatuses.has(row.status));
      return textMatches && statusMatches;
    });
    document.getElementById('summary').innerHTML = `${data.verdict ? `<span>${status(data.verdict)}</span>` : `<span>${visible.length} acceptance conditions</span>`}${gateNotices}`;
    document.getElementById('report').innerHTML = visible.map(ac => `<article class="ac" tabindex="0"><p class="eyebrow">${e(ac.acId)}</p><h2>${e(ac.title)}</h2>${(ac.specNarrative || []).length ? `<section class="section external"><h3>External spec narrative</h3>${markdown(ac.specNarrative)}</section>` : ''}${scenario(scenarioFor(ac))}<section class="section"><h3>Case matrix</h3>${renderCases(ac)}</section>${projection === 'review' && ac.method?.sourceCode ? `<details class="source"><summary>Canonical Java source</summary><pre>${e(ac.method.sourceCode)}</pre></details>` : ''}</article>`).join('') || '<p class="meta">No acceptance conditions match this query.</p>';
  };
  document.getElementById('query').addEventListener('input', render); render();
})();
