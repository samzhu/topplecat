(() => {
  const data = JSON.parse(document.getElementById('topplecat-report-data').textContent);
  const projection = data.schemaVersion.includes('review')
    ? 'review'
    : data.schemaVersion.includes('verification') ? 'verification' : 'spec';
  const e = value => String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character]);
  const pretty = value => e(JSON.stringify(value, null, 2));
  const status = value => `<span class="badge ${e(value)}">${e(value)}</span>`;
  const markdown = blocks => (blocks || []).map(block => block.kind === 'LIST'
    ? `<ul>${(block.items || []).map(item => `<li>${e(item)}</li>`).join('')}</ul>`
    : `<p>${e(block.text)}</p>`).join('');

  const scenario = steps => steps?.length
    ? `<section class="contract-panel scenario-panel">
        <p class="panel-label">Executable scenario</p>
        <ol class="scenario">${steps.map(step => `<li>${e(step)}</li>`).join('')}</ol>
      </section>`
    : '';

  const JAVA_KEYWORDS = new Set([
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
    'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final',
    'finally', 'float', 'for', 'if', 'implements', 'import', 'instanceof', 'int', 'interface',
    'long', 'native', 'new', 'package', 'private', 'protected', 'public', 'record', 'return',
    'sealed', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
    'throw', 'throws', 'transient', 'try', 'var', 'void', 'volatile', 'while', 'yield'
  ]);

  const highlighted = (kind, value) => `<span class="tok-${kind}">${e(value)}</span>`;

  const highlightJava = sourceCode => {
    const source = String(sourceCode ?? '');
    let output = '';
    let index = 0;
    while (index < source.length) {
      const rest = source.slice(index);
      let match;
      if (rest.startsWith('//')) {
        const end = source.indexOf('\n', index);
        const boundary = end < 0 ? source.length : end;
        output += highlighted('comment', source.slice(index, boundary));
        index = boundary;
      } else if (rest.startsWith('/*')) {
        const end = source.indexOf('*/', index + 2);
        const boundary = end < 0 ? source.length : end + 2;
        output += highlighted('comment', source.slice(index, boundary));
        index = boundary;
      } else if (rest.startsWith('"""')) {
        const end = source.indexOf('"""', index + 3);
        const boundary = end < 0 ? source.length : end + 3;
        output += highlighted('string', source.slice(index, boundary));
        index = boundary;
      } else if (source[index] === '"' || source[index] === "'") {
        const quote = source[index];
        let boundary = index + 1;
        let escaped = false;
        while (boundary < source.length) {
          const character = source[boundary++];
          if (escaped) {
            escaped = false;
          } else if (character === '\\') {
            escaped = true;
          } else if (character === quote) {
            break;
          }
        }
        output += highlighted('string', source.slice(index, boundary));
        index = boundary;
      } else if ((match = rest.match(/^@[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*/))) {
        output += highlighted('annotation', match[0]);
        index += match[0].length;
      } else if ((match = rest.match(/^(?:0[xX][\da-fA-F_]+|0[bB][01_]+|\d[\d_]*(?:\.\d[\d_]*)?)(?:[eE][+-]?\d[\d_]*)?[fFdDlL]?/))) {
        output += highlighted('number', match[0]);
        index += match[0].length;
      } else if ((match = rest.match(/^[A-Za-z_$][\w$]*/))) {
        const word = match[0];
        const kind = JAVA_KEYWORDS.has(word)
          ? 'keyword'
          : /^[A-Z]/.test(word) ? 'type' : '';
        output += kind ? highlighted(kind, word) : e(word);
        index += word.length;
      } else {
        output += e(source[index]);
        index++;
      }
    }
    return output;
  };

  const attachmentPath = item => /^attachments\/[a-f0-9]{64}\.(png|jpg|json|txt)$/
    .test(item?.relativePath || '') ? item.relativePath : '';
  const attachments = rows => rows?.length
    ? `<div class="attachments">${rows.map(item => {
        const rawPath = attachmentPath(item);
        if (!rawPath) return '';
        const path = e(rawPath);
        const label = e(item.title);
        return item.mediaType?.startsWith('image/')
          ? `<a href="${path}" target="_blank" rel="noopener"><img src="${path}" alt="${label}"><span>${label}</span></a>`
          : `<a href="${path}" target="_blank" rel="noopener">${label} (${e(item.mediaType)})</a>`;
      }).join('')}</div>`
    : '';
  const source = (step, sources) => {
    const reference = sources?.[step.stepId];
    return reference
      ? `<p class="source-ref">Source: ${e(reference.file)}:${e(reference.line)}:${e(reference.column)}</p>`
      : '';
  };
  const steps = (rows, sources) => rows?.length
    ? `<ol class="steps">${rows.map(step => `<li class="${e(step.status)}">
        ${e(step.sentence)} ${status(step.status)}
        ${step.durationNanos ? `<span class="duration">${(step.durationNanos / 1e6).toFixed(1)} ms</span>` : ''}
        ${source(step, sources)}
        ${step.actualArguments?.length ? `<pre class="json">${pretty(step.actualArguments)}</pre>` : ''}
        ${step.failureRef ? `<pre class="failure">${e(step.failureRef)}</pre>` : ''}
        ${attachments(step.attachments)}
      </li>`).join('')}</ol>`
    : '';

  const caseMatrix = (row, markers = '') => `<article class="case" tabindex="0">
    <h4>${e(row.caseId)} ${markers}</h4>
    <div class="case-grid">
      <div><p class="meta">Inputs</p><pre class="json">${pretty(row.inputs)}</pre></div>
      <div><p class="meta">Expected</p><pre class="json">${pretty(row.expected)}</pre></div>
    </div>
  </article>`;
  const renderSpecCases = ac => (ac.publicCases || []).map(row => caseMatrix(row)).join('');
  const visibilityBadge = visibility => visibility === 'PUBLIC'
    ? '<span class="badge PUBLIC">Public example</span>'
    : visibility === 'HIDDEN'
      ? '<span class="badge HIDDEN">Reviewer-only check</span>'
      : `<span class="badge">${e(visibility)}</span>`;
  const reviewCaseGroup = (title, explanation, rows, badgeClass, badgeLabel) => rows.length
    ? `<section class="case-group">
        <div class="case-group-heading">
          <div><h4>${e(title)}</h4><p>${e(explanation)}</p></div>
          <span class="case-count">${rows.length}</span>
        </div>
        ${rows.map(row => caseMatrix(row,
          `<span class="badge ${badgeClass}">${e(badgeLabel)}</span>`)).join('')}
      </section>`
    : '';
  const renderReviewCases = ac => {
    const rows = ac.cases || [];
    const publicRows = rows.filter(row => row.visibility === 'PUBLIC');
    const reviewerRows = rows.filter(row => row.visibility === 'HIDDEN');
    return reviewCaseGroup(
      'Public examples',
      'These examples are part of the implementation handoff.',
      publicRows,
      'PUBLIC',
      'Public example'
    ) + reviewCaseGroup(
      'Reviewer-only checks',
      'These independently derived examples retest the same AC and stay out of the implementation handoff. They do not add a new requirement.',
      reviewerRows,
      'HIDDEN',
      'Reviewer-only check'
    );
  };
  const renderVerificationCases = ac => (ac.cases || []).map(row => `<article class="case" tabindex="0">
    <h4>${e(row.caseId)} ${visibilityBadge(row.visibility)} ${status(row.status)}</h4>
    <div class="case-grid">
      <div><p class="meta">Inputs</p><pre class="json">${pretty(row.inputs)}</pre></div>
      <div><p class="meta">Expected</p><pre class="json">${pretty(row.expected)}</pre></div>
    </div>
    <div class="section"><p class="meta">Expected consumption</p><pre class="json">${pretty(row.expectedConsumption)}</pre></div>
    ${row.steps.length ? `<div class="section"><h4>Step evidence</h4>${steps(row.steps, ac.stepSources)}</div>` : ''}
    ${row.failure ? `<pre class="failure">${e(row.failure)}</pre>` : ''}
  </article>`).join('');
  const caseRows = ac => projection === 'spec' ? (ac.publicCases || []) : (ac.cases || []);
  const renderCases = ac => projection === 'spec'
    ? renderSpecCases(ac)
    : projection === 'review' ? renderReviewCases(ac) : renderVerificationCases(ac);
  const scenarioFor = ac => projection === 'review' ? (ac.method?.staticSentences || []) : (ac.scenario || []);
  const canonicalMethod = method => method?.sourceCode
    ? `<section class="contract-panel method-panel">
        <p class="panel-label">Canonical acceptance method</p>
        <p class="panel-help">This is the public <code>@ToppleTest</code> method bound to this AC.</p>
        <pre class="java"><code>${highlightJava(method.sourceCode)}</code></pre>
      </section>`
    : '';

  const title = projection === 'review'
    ? 'Contract review'
    : projection === 'verification' ? 'Verification evidence' : 'Public contract';
  document.getElementById('title').textContent = title;
  document.getElementById('notice').textContent = projection === 'review'
    ? 'Before handoff, compare the public implementation contract with reviewer-only checks for the same acceptance condition.'
    : projection === 'verification'
      ? `Suite verdict: ${data.verdict}`
      : 'Public contract projection. It contains no reviewer data or execution failure.';

  const acceptanceConditions = data.acceptanceConditions || [];
  const filters = document.getElementById('filters');
  const enabledStatuses = new Set();
  const statuses = ['PASS', 'FAIL', 'NOT_REPORTED'];
  if (projection === 'verification') {
    filters.innerHTML = statuses.map(value =>
      `<button type="button" data-status="${value}" aria-pressed="false">${value}</button>`).join('');
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
    filters.addEventListener('click', event => toggleStatus(event.target?.dataset?.status));
    filters.addEventListener('keydown', event => {
      if (event.key !== 'Enter' && event.key !== ' ') return;
      const value = event.target?.dataset?.status;
      if (!value) return;
      event.preventDefault();
      toggleStatus(value);
    });
  }

  const gateNotices = (data.gates || []).filter(gate => gate.verdict !== 'PASS').map(gate =>
    `<p class="gate-notice ${e(gate.verdict)}">${e(gate.name)}: ${status(gate.verdict)} ${e(gate.reason || '')}</p>`
  ).join('');
  const reviewSummary = visible => {
    const rows = visible.flatMap(ac => ac.cases || []);
    const publicCount = rows.filter(row => row.visibility === 'PUBLIC').length;
    const reviewerCount = rows.filter(row => row.visibility === 'HIDDEN').length;
    return `<span>${visible.length} acceptance conditions</span>
      <span class="PUBLIC">${publicCount} public examples</span>
      <span class="HIDDEN">${reviewerCount} reviewer-only checks</span>`;
  };

  const render = () => {
    const query = document.getElementById('query').value.trim().toLowerCase();
    const visible = acceptanceConditions.filter(ac => {
      const rows = caseRows(ac);
      const textMatches = !query
        || `${ac.acId} ${ac.title} ${rows.map(testCase => testCase.caseId).join(' ')}`
          .toLowerCase().includes(query);
      const statusMatches = !enabledStatuses.size
        || enabledStatuses.has(ac.status)
        || rows.some(row => enabledStatuses.has(row.status));
      return textMatches && statusMatches;
    });
    document.getElementById('summary').innerHTML = projection === 'review'
      ? reviewSummary(visible)
      : `${data.verdict ? `<span>${status(data.verdict)}</span>` : `<span>${visible.length} acceptance conditions</span>`}${gateNotices}`;
    document.getElementById('report').innerHTML = visible.map(ac => `<article class="ac" tabindex="0">
      <p class="eyebrow">${e(ac.acId)}</p>
      <h2>${e(ac.title)}</h2>
      ${(ac.specNarrative || []).length
        ? `<section class="section external"><h3>External spec narrative</h3>${markdown(ac.specNarrative)}</section>`
        : ''}
      ${projection === 'review'
        ? `<div class="contract-map">${scenario(scenarioFor(ac))}${canonicalMethod(ac.method)}</div>`
        : scenario(scenarioFor(ac))}
      <section class="section cases-section">
        <h3>${projection === 'review' ? 'Contract examples' : 'Case matrix'}</h3>
        ${renderCases(ac)}
      </section>
    </article>`).join('') || '<p class="meta">No acceptance conditions match this query.</p>';
  };

  document.getElementById('query').addEventListener('input', render);
  render();
})();
