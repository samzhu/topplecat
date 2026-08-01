(() => {
  const data = JSON.parse(document.getElementById('topplecat-report-data').textContent);
  const projection = data.schemaVersion.includes('review')
    ? 'review' : data.schemaVersion.includes('verification') ? 'verification' : 'spec';
  const e = value => String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character]);
  const status = value => `<span class="badge ${e(value)}">${e(value)}</span>`;
  const pretty = value => e(JSON.stringify(value, null, 2));
  const markdown = blocks => (blocks || []).map(block => block.kind === 'LIST'
    ? `<ul>${(block.items || []).map(item => `<li>${e(item)}</li>`).join('')}</ul>`
    : `<p>${e(block.text)}</p>`).join('');
  const label = phase => ({ GIVEN: 'Given', WHEN: 'When', THEN: 'Then', AND: 'And' })[phase] || 'And';
  const stripPhase = text => String(text || '').replace(/^(Given|When|Then|And)\s+/i, '');
  const parseLegacy = text => {
    const match = String(text || '').match(/^(Given|When|Then|And)\s+(.*)$/i);
    return match ? { phase: match[1].toUpperCase(), sentence: match[2] } : { phase: 'AND', sentence: text };
  };
  const presentationSteps = rows => {
    let previous = '';
    return (rows || []).map(row => {
      const source = typeof row === 'string' ? parseLegacy(row) : row;
      const phase = source.phase || 'AND';
      const shown = previous === phase && phase !== 'AND' ? 'AND' : phase;
      previous = phase;
      return { ...source, phase: shown, sentence: stripPhase(source.sentence) };
    });
  };
  const scenario = (rows, sources, live) => {
    const steps = presentationSteps(rows);
    if (!steps.length) return '';
    return `<section class="scenario-panel" aria-label="Given When Then scenario">
      <p class="panel-label">${live ? 'Executed scenario' : 'Scenario for this case'}</p>
      <div class="scenario">${steps.map(step => `<div class="scenario-step ${e(step.status || '')}">
        <span class="scenario-phase">${e(label(step.phase))}</span>
        <div class="scenario-sentence">${e(step.sentence)}
          ${step.status ? ` ${status(step.status)}` : ''}
          ${step.durationNanos ? `<span class="duration">${(step.durationNanos / 1e6).toFixed(1)} ms</span>` : ''}
          ${sources?.[step.stepId] ? `<span class="source-ref">${e(sources[step.stepId].file)}:${e(sources[step.stepId].line)}</span>` : ''}
          ${step.failureRef ? `<pre class="failure">${e(step.failureRef)}</pre>` : ''}
          ${attachments(step.attachments)}
        </div>
      </div>`).join('')}</div>
    </section>`;
  };

  const JAVA_KEYWORDS = new Set(['abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
    'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for',
    'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new', 'package', 'private',
    'protected', 'public', 'record', 'return', 'sealed', 'short', 'static', 'strictfp', 'super', 'switch',
    'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'var', 'void', 'volatile', 'while', 'yield']);
  const highlighted = (kind, value) => `<span class="tok-${kind}">${e(value)}</span>`;
  const highlightJava = sourceCode => {
    const source = String(sourceCode ?? '');
    let output = ''; let index = 0;
    while (index < source.length) {
      const rest = source.slice(index); let match;
      if (rest.startsWith('//')) {
        const end = source.indexOf('\n', index); const boundary = end < 0 ? source.length : end;
        output += highlighted('comment', source.slice(index, boundary)); index = boundary;
      } else if (rest.startsWith('/*')) {
        const end = source.indexOf('*/', index + 2); const boundary = end < 0 ? source.length : end + 2;
        output += highlighted('comment', source.slice(index, boundary)); index = boundary;
      } else if (rest.startsWith('"""')) {
        const end = source.indexOf('"""', index + 3); const boundary = end < 0 ? source.length : end + 3;
        output += highlighted('string', source.slice(index, boundary)); index = boundary;
      } else if (source[index] === '"' || source[index] === "'") {
        const quote = source[index]; let boundary = index + 1; let escaped = false;
        while (boundary < source.length) {
          const character = source[boundary++];
          if (escaped) escaped = false;
          else if (character === '\\') escaped = true;
          else if (character === quote) break;
        }
        output += highlighted('string', source.slice(index, boundary)); index = boundary;
      } else if ((match = rest.match(/^@[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*/))) {
        output += highlighted('annotation', match[0]); index += match[0].length;
      } else if ((match = rest.match(/^(?:0[xX][\da-fA-F_]+|0[bB][01_]+|\d[\d_]*(?:\.\d[\d_]*)?)(?:[eE][+-]?\d[\d_]*)?[fFdDlL]?/))) {
        output += highlighted('number', match[0]); index += match[0].length;
      } else if ((match = rest.match(/^[A-Za-z_$][\w$]*/))) {
        const word = match[0]; const kind = JAVA_KEYWORDS.has(word) ? 'keyword' : /^[A-Z]/.test(word) ? 'type' : '';
        output += kind ? highlighted(kind, word) : e(word); index += word.length;
      } else { output += e(source[index]); index++; }
    }
    return output;
  };

  const attachmentPath = item => /^attachments\/[a-f0-9]{64}\.(png|jpg|json|txt)$/.test(item?.relativePath || '')
    ? item.relativePath : '';
  const attachments = rows => rows?.length ? `<div class="attachments">${rows.map(item => {
    const rawPath = attachmentPath(item); if (!rawPath) return '';
    const path = e(rawPath); const itemLabel = e(item.title);
    return item.mediaType?.startsWith('image/')
      ? `<a href="${path}" target="_blank" rel="noopener"><img src="${path}" alt="${itemLabel}"><span>${itemLabel}</span></a>`
      : `<a href="${path}" target="_blank" rel="noopener">${itemLabel} (${e(item.mediaType)})</a>`;
  }).join('')}</div>` : '';

  const valueText = value => typeof value === 'string' ? value : JSON.stringify(value);
  const flattened = (value, path = '') => {
    if (Array.isArray(value)) {
      return value.length ? value.flatMap((item, index) => flattened(item, `${path}[${index}]`)) : [[path, '[]']];
    }
    if (value && typeof value === 'object') {
      const keys = Object.keys(value);
      return keys.length ? keys.flatMap(key => flattened(value[key], path ? `${path}.${key}` : key)) : [[path, '{}']];
    }
    return [[path || 'value', valueText(value)]];
  };
  const keyValues = (value, compact = false) => `<dl class="key-values ${compact ? 'compact' : ''}">${flattened(value)
    .map(([path, item]) => `<div><dt>${e(path)}</dt><dd>${e(item)}</dd></div>`).join('')}</dl>`;
  const visibility = row => row.visibility === 'HIDDEN'
    ? '<span class="badge HIDDEN">Reviewer case</span>' : '<span class="badge PUBLIC">Public case</span>';
  const rawCase = row => `<details class="raw-case"><summary>View raw case data</summary><pre class="json">${pretty({
    caseId: row.caseId, inputs: row.inputs, expected: row.expected
  })}</pre></details>`;
  const caseRows = ac => projection === 'spec'
    ? (ac.publicCases || []).map(row => ({ ...row, visibility: 'PUBLIC', scenario: [] })) : (ac.cases || []);
  const defaultCase = rows => rows.find(row => row.visibility === 'PUBLIC') || rows[0];
  const selectedCases = new Map();
  const expanded = new Set();
  const selected = ac => {
    const rows = caseRows(ac); const desired = rows.find(row => row.caseId === selectedCases.get(ac.acId));
    const current = desired || defaultCase(rows);
    if (current) selectedCases.set(ac.acId, current.caseId);
    return current;
  };
  const selector = (ac, current) => {
    const rows = caseRows(ac); if (!rows.length) return '';
    return `<section class="case-selector-section"><p class="panel-label">Case selector</p>
      <div class="case-selector" role="tablist" aria-label="Cases for ${e(ac.acId)}">${rows.map(row => `<button type="button"
        role="tab" aria-selected="${row.caseId === current.caseId}" data-case-select data-ac="${e(ac.acId)}"
        data-case="${e(row.caseId)}">${visibility(row)} <span>${e(row.caseId)}</span></button>`).join('')}</div></section>`;
  };
  const reviewScenario = (ac, current) => current?.scenario?.length
    ? scenario(current.scenario, null, false) : scenario(ac.method?.staticSentences || [], null, false);
  const verificationScenario = (ac, current) => current?.steps?.length
    ? scenario(current.steps.map(step => ({ ...step, phase: ac.stepPhases?.[step.stepId] || 'AND' })), ac.stepSources, true)
    : scenario(ac.scenario || [], null, false);
  const caseDetail = (ac, current) => {
    if (!current) return '<p class="meta">This acceptance condition has no cases.</p>';
    const scenarioMarkup = projection === 'verification' ? verificationScenario(ac, current)
      : projection === 'review' ? reviewScenario(ac, current) : scenario(ac.scenario || [], null, false);
    return `<section class="case-detail" role="tabpanel" aria-label="Selected case ${e(current.caseId)}">
      <div class="case-detail-heading"><div><p class="panel-label">Selected case</p><h3>${e(current.caseId)} ${visibility(current)}</h3></div>
      ${projection === 'verification' ? status(current.status) : ''}</div>
      ${scenarioMarkup}
      <div class="io-grid"><section><h4>Inputs</h4>${keyValues(current.inputs)}</section>
        <section><h4>Expected output</h4>${keyValues(current.expected)}</section></div>
      ${projection === 'verification' ? `<section class="consumption"><h4>Expected consumption</h4>${keyValues(current.expectedConsumption || {})}
        ${current.failure ? `<pre class="failure">${e(current.failure)}</pre>` : ''}</section>` : ''}
      ${rawCase(current)}
    </section>`;
  };
  const matrix = ac => {
    const rows = caseRows(ac); if (!rows.length) return '';
    return `<section class="case-matrix-section"><h3>All cases</h3><div class="table-scroll"><table class="case-matrix">
      <thead><tr><th>Type</th><th>Case ID</th><th>Inputs</th><th>Expected output</th></tr></thead>
      <tbody>${rows.map(row => `<tr tabindex="0" role="button" data-case-select data-ac="${e(ac.acId)}" data-case="${e(row.caseId)}"
        aria-label="Select case ${e(row.caseId)}" class="${selected(ac)?.caseId === row.caseId ? 'selected' : ''}">
        <td>${visibility(row)}</td><td>${e(row.caseId)}</td><td>${keyValues(row.inputs, true)}</td><td>${keyValues(row.expected, true)}</td></tr>`).join('')}</tbody>
    </table></div></section>`;
  };
  const canonicalMethod = method => method?.sourceCode ? `<details class="source method-panel">
    <summary>View matching <code>@ToppleAcceptanceTest</code></summary><p class="panel-help">Only the acceptance method bound to this AC is shown.</p>
    <pre class="java"><code>${highlightJava(method.sourceCode)}</code></pre></details>` : '';
  const counts = ac => {
    const rows = caseRows(ac); const publicCount = rows.filter(row => row.visibility === 'PUBLIC').length;
    return `<span>${publicCount} public cases</span><span>${rows.length - publicCount} reviewer cases</span><span>${(ac.properties || []).length} properties</span>`;
  };
  const propertyLabel = () => '<span class="badge PUBLIC">Property-Based Testing</span>';
  const propertyStatic = property => `<p class="meta"><code>${e(property.methodIdentity)}</code> · ${e(property.tries)} trials · max discards ${e(property.maxDiscards)} · max shrinks ${e(property.maxShrinks)}
    ${property.sourceFile ? ` · ${e(property.sourceFile)}:${e(property.sourceLine)}` : ''}</p>`;
  const counterexample = (heading, value) => value ? `<section class="counterexample"><h5>${heading}</h5><pre class="json">${e(value.choicesJson)}</pre>
    ${value.shrinkPath?.length ? `<p class="meta">Shrink path: ${e(value.shrinkPath.join(', '))}</p>` : ''}</section>` : '';
  const verificationProperty = property => `<article class="property-card ${e(property.status)}">
    <div class="case-detail-heading"><div><h4>${e(property.title)}</h4>${propertyLabel()}</div>${status(property.status)}</div>
    ${propertyStatic(property)}<p class="meta">Completed ${e(property.completedTrials)}/${e(property.requestedTrials)} trials · edges ${e(property.edgeTrials)} · random ${e(property.randomTrials)} · discards ${e(property.discards)} · seed <code>${e(property.seed)}</code> · replay ${property.replayVerified ? 'verified' : 'not verified'}</p>
    ${(property.classifications || []).length ? `<div class="table-scroll"><table class="case-matrix"><thead><tr><th>Classification</th><th>Count</th><th>Percent</th><th>Required</th></tr></thead><tbody>${property.classifications.map(item => `<tr><td>${e(item.label)}</td><td>${e(item.count)}</td><td>${e(item.percent.toFixed(2))}%</td><td>${item.minimumPercent == null ? '—' : `${e(item.minimumPercent)}%`}</td></tr>`).join('')}</tbody></table></div>` : ''}
    ${counterexample('Original counterexample', property.originalCounterexample)}${counterexample('Shrunk counterexample', property.shrunkCounterexample)}
    ${property.replayToken ? `<p class="meta">Replay token <code>${e(property.replayToken)}</code></p>` : ''}${property.incompleteReason ? `<p class="gate-notice INCOMPLETE">${e(property.incompleteReason)}</p>` : ''}
  </article>`;
  const propertyCards = ac => {
    const properties = ac.properties || []; if (!properties.length) return '';
    if (projection === 'verification') return `<section class="property-section"><h3>Property-Based Testing results</h3>${properties.map(verificationProperty).join('')}</section>`;
    if (projection === 'review') return `<section class="property-section"><h3>Property-Based Testing</h3><p class="panel-help">Properties are supplementary safeguards and do not replace typed acceptance examples.</p>${properties.map(property => `<article class="property-card"><h4>${e(property.title)} ${propertyLabel()}</h4>${propertyStatic(property)}${property.sourceCode ? `<details class="source method-panel"><summary>View matching <code>@ToppleProperty</code></summary><pre class="java"><code>${highlightJava(property.sourceCode)}</code></pre></details>` : ''}</article>`).join('')}</section>`;
    return `<section class="property-section"><h3>Property-Based Testing</h3>${properties.map(property => `<article class="property-card"><h4>${e(property.title)}</h4>${propertyStatic(property)}</article>`).join('')}</section>`;
  };
  const pitOutcomeTable = (heading, outcomes) => !outcomes?.length ? '' : `<section class="mutation-outcomes"><h4>${e(heading)}</h4>
    <div class="table-scroll"><table class="case-matrix"><thead><tr><th>PIT status</th><th>PIT detected</th><th>Mutants</th></tr></thead>
    <tbody>${outcomes.map(outcome => `<tr><td><code>${e(outcome.status)}</code></td><td>${e(outcome.detected)}</td><td>${e(outcome.count)}</td></tr>`).join('')}</tbody></table></div></section>`;
  const selectorList = selectors => selectors?.length ? `<ul>${selectors.map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul>` : '<p class="meta">None reported by PIT.</p>';
  const mutationSummary = () => {
    if (projection !== 'verification') return '';
    const mutation = data.mutationAttribution;
    const mutationGate = (data.gates || []).find(gate => gate.name === 'MUTATION');
    const gateVerdict = status(mutationGate?.verdict || 'INCOMPLETE');
    const gateReason = mutationGate?.reason ? `<p class="meta">${e(mutationGate.reason)}</p>` : '';
    if (!mutation) return `<section class="mutation-summary"><h2>Mutation Testing</h2><p>${gateVerdict}</p>${gateReason}
      <p class="panel-help">No current managed PIT attribution was available for this formal Verify.</p></section>`;
    const assessments = mutation.assessments || [];
    const profile = (mutation.managedOperatorIds || []).map(operator => `<li><code>${e(operator)}</code></li>`).join('');
    const mutators = mutation.perMutatorSummaries || [];
    return `<section class="mutation-summary"><h2>Mutation Testing</h2><p>${gateVerdict}</p>${gateReason}
      <p class="panel-help">PIT’s <code>status</code> and <code>detected</code> values are shown unchanged; see <a href="https://pitest.org/quickstart/basic_concepts/" target="_blank" rel="noopener">PIT’s official mutation outcome definitions</a>. ToppleCat’s contract-scoped detection rate is based only on the exact Acceptance Method selectors in PIT <code>killingTests</code>, divided by its exact <code>coveringTests</code> selectors.</p>
      <p>PIT <code>${e(mutation.pitVersion)}</code> · managed profile <code>${e(mutation.managedProfileId)}</code></p>
      <section><h3>Managed operator IDs</h3><ul>${profile}</ul></section>
      <p><strong>${e(mutation.producerMutationCount)}</strong> producer mutants · <strong>${e(mutation.uniquelyAttributedMutationCount)}</strong> uniquely attributed to public Acceptance Methods · <strong>${e(mutation.unattributedMutationCount)}</strong> unattributed</p>
      ${pitOutcomeTable('All producer outcomes', mutation.producerOutcomeCounts)}${pitOutcomeTable('Unattributed producer outcomes', mutation.unattributedOutcomeCounts)}
      ${mutators.length ? `<section><h3>Per-mutator summary</h3><div class="table-scroll"><table class="case-matrix"><thead><tr><th>Raw PIT mutator</th><th>Mutants</th><th>Raw outcomes</th></tr></thead><tbody>${mutators.map(summary => `<tr><td><code>${e(summary.mutator)}</code></td><td>${e(summary.mutantCount)}</td><td>${e((summary.outcomeCounts || []).map(outcome => `${outcome.status}/${outcome.detected}: ${outcome.count}`).join(', '))}</td></tr>`).join('')}</tbody></table></div></section>` : ''}
      <section><h3>Per-AC contract detection</h3><div class="table-scroll"><table class="case-matrix"><thead><tr><th>AC</th><th>Covered mutants</th><th>Detected by this Acceptance Method</th><th>Sealed threshold</th><th>Detection rate</th></tr></thead>
      <tbody>${assessments.map(assessment => `<tr><td>${e(assessment.acId)}</td><td>${e(assessment.coveredMutantCount)}</td><td>${e(assessment.killedByAcceptanceMethodMutantCount)}</td><td>${e(assessment.sealedThreshold)}%</td><td>${e(assessment.detectionRate)}%</td></tr>`).join('')}</tbody></table></div>${assessments.filter(assessment => assessment.attributionGap).map(assessment => `<p class="panel-help"><code>${e(assessment.acId)}</code>：此 AC 沒有取得本次 managed mutation profile 的歸因證據，需要 reviewer 判斷。</p>`).join('')}</section>
      ${assessments.map(assessment => pitOutcomeTable(`${assessment.acId} PIT outcomes`, assessment.pitOutcomeCounts)).join('')}
      <details class="raw-case"><summary>View raw PIT mutant details</summary>${(mutation.mutations || []).map((item, index) => `<section class="mutation-selector"><h4>Mutant ${index + 1}: <code>${e(item.status)}</code> · detected ${e(item.detected)}</h4><p class="meta">Mutated class: <code>${e(item.mutatedClass)}</code> · raw mutator: <code>${e(item.mutator)}</code> · attributed ACs: ${e((item.attributedAcceptanceConditionIds || []).join(', ') || 'none')}</p><p>${e(item.description)}</p><h5>coveringTests</h5>${selectorList(item.coveringTests)}<h5>killingTests</h5>${selectorList(item.killingTests)}<h5>succeedingTests</h5>${selectorList(item.succeedingTests)}</section>`).join('')}</details>
    </section>`;
  };
  const functionalSafeguardSections = () => {
    if (projection !== 'verification') return '';
    const gates = new Map((data.gates || []).map(gate => [gate.name, gate]));
    const safeguard = (heading, gateName, explanation) => {
      const gate = gates.get(gateName); const verdict = gate ? status(gate.verdict) : status('INCOMPLETE');
      const reason = gate?.reason ? `<p class="meta">${e(gate.reason)}</p>` : '';
      return `<section class="safeguard-summary"><h2>${heading}</h2><p>${verdict}</p><p class="panel-help">${explanation}</p>${reason}</section>`;
    };
    const integrity = gates.get('CONTRACT_INTEGRITY');
    return `<section class="mechanical-seal-summary"><h2>Mechanical Seal / Contract Integrity</h2><p>${status(integrity?.verdict || 'INCOMPLETE')}</p>${integrity?.reason ? `<p class="meta">${e(integrity.reason)}</p>` : ''}</section>
      ${safeguard('Hidden Tests', 'REVIEWER_JUNIT', 'Reviewer-owned typed rows provide evidence only for Hidden Tests.')}
      ${safeguard('Property-Based Testing', 'PROPERTY', 'Bounded Properties provide evidence only for Property-Based Testing.')}
      ${mutationSummary()}`;
  };
  const contractQualityAdvisories = () => {
    const advisories = data.contractQualityAdvisories || []; if (projection !== 'review' || !advisories.length) return '';
    return `<section class="contract-quality-advisories"><h2>Contract quality advisory</h2>
      <p class="panel-help">These reviewer-only observations are non-blocking. They do not add a business rule, alter the executable contract, or change a Gate.</p>
      <div class="table-scroll"><table class="case-matrix"><thead><tr><th>Rule</th><th>AC</th><th>Expected path</th><th>Public count</th><th>Reviewer count</th></tr></thead><tbody>
      ${advisories.map(advisory => `<tr><td><code>${e(advisory.ruleCode)}</code></td><td>${e(advisory.acId)}</td><td><code>${e(advisory.expectedPath)}</code></td><td>${e(advisory.publicCount)}</td><td>${e(advisory.hiddenCount)}</td></tr>`).join('')}
      </tbody></table></div></section>`;
  };
  const title = projection === 'review' ? 'Contract review' : projection === 'verification' ? 'Verification evidence' : 'Public contract';
  document.getElementById('title').textContent = title;
  document.getElementById('notice').textContent = projection === 'review'
    ? 'Review the business scenario, one selected case, its inputs and expected output before reading source.'
    : projection === 'verification' ? `Suite verdict: ${data.verdict}`
      : 'Public contract projection. It contains no reviewer data or execution failure.';
  const acceptanceConditions = data.acceptanceConditions || [];
  const filters = document.getElementById('filters');
  const enabledStatuses = new Set();
  if (acceptanceConditions[0]) expanded.add(acceptanceConditions[0].acId);
  filters.innerHTML = `<button type="button" data-action="expand">Expand all</button><button type="button" data-action="collapse">Collapse all</button>${projection === 'verification'
    ? ['PASS', 'FAIL', 'NOT_REPORTED'].map(value => `<button type="button" data-status="${value}" aria-pressed="false">${value}</button>`).join('') : ''}`;
  const gateNotices = (data.gates || []).filter(gate => gate.verdict !== 'PASS').map(gate =>
    `<p class="gate-notice ${e(gate.verdict)}">${e(gate.name)}: ${status(gate.verdict)} ${e(gate.reason || '')}</p>`).join('');
  const scopeSummary = () => {
    const scope = data.deliveryScope; if (!scope) return '';
    const documents = (scope.specDocuments || []).map(document =>
      `<li><code>${e(document.path)}</code> <span class="meta">${e(document.sha256)}</span></li>`).join('')
      || '<li>Compatibility scope: no external Spec document was selected.</li>';
    const acIds = (scope.acceptanceConditionIds || []).join(', ') || 'All existing contract ACs';
    return `<section class="scope-summary"><p class="panel-label">Delivery scope</p>
      <p><strong>${(scope.acceptanceConditionIds || []).length}</strong> selected ACs: ${e(acIds)}</p>
      <p>AC set digest: <code>${e(scope.acceptanceConditionSetDigest)}</code></p>
      <ul>${documents}</ul>
      ${(scope.reviewerWarnings || []).map(warning => `<p class="gate-notice INCOMPLETE">${e(warning)}</p>`).join('')}
      ${projection === 'verification' ? `<p>Hidden Tests: <strong>${e(scope.hiddenMode)}</strong> (${e(scope.executedHiddenRows)} rows) · Property-Based Testing: <strong>${e(scope.publicPropertyMode)}</strong> (${e(scope.executedPublicProperties)} properties) · Mutation Testing: <strong>${e(scope.mutationMode)}</strong></p>` : ''}
    </section>`;
  };
  const render = () => {
    const query = document.getElementById('query').value.trim().toLowerCase();
    const visible = acceptanceConditions.filter(ac => {
      const rows = caseRows(ac);
      const textMatches = !query || `${ac.acId} ${ac.title} ${rows.map(row => row.caseId).join(' ')} ${(ac.properties || []).map(property => `${property.title} ${property.methodIdentity} ${(property.classifications || []).map(item => item.label).join(' ')}`).join(' ')}`.toLowerCase().includes(query);
      const statusMatches = !enabledStatuses.size || enabledStatuses.has(ac.status) || rows.some(row => enabledStatuses.has(row.status));
      return textMatches && statusMatches;
    });
    if (query) visible.forEach(ac => expanded.add(ac.acId));
    document.getElementById('summary').innerHTML = projection === 'review'
      ? `${scopeSummary()}<span>${visible.length} acceptance conditions</span>${visible.map(counts).join('')}${contractQualityAdvisories()}`
      : `${scopeSummary()}${data.verdict ? `<span>${status(data.verdict)}</span>` : `<span>${visible.length} acceptance conditions</span>`}${gateNotices}${functionalSafeguardSections()}`;
    document.getElementById('report').innerHTML = visible.map(ac => {
      const current = selected(ac); const isOpen = expanded.has(ac.acId);
      return `<details class="ac" data-ac-details="${e(ac.acId)}" ${isOpen ? 'open' : ''}><summary>
        <span><span class="eyebrow">${e(ac.acId)}</span><strong>${e(ac.title)}</strong></span><span class="ac-counts">${counts(ac)}</span>
      </summary><div class="ac-body">
        ${(ac.specNarrative || []).length ? `<section class="section external"><h3>External spec narrative</h3>${markdown(ac.specNarrative)}</section>` : ''}
        ${selector(ac, current)}${caseDetail(ac, current)}${matrix(ac)}${projection === 'review' ? canonicalMethod(ac.method) : ''}${propertyCards(ac)}
      </div></details>`;
    }).join('') || '<p class="meta">No acceptance conditions match this query.</p>';
  };
  const selectCase = target => {
    const choice = target.closest?.('[data-case-select]'); if (!choice) return false;
    selectedCases.set(choice.dataset.ac, choice.dataset.case); expanded.add(choice.dataset.ac); render(); return true;
  };
  filters.addEventListener('click', event => {
    const action = event.target?.dataset?.action;
    if (action === 'expand') { acceptanceConditions.forEach(ac => expanded.add(ac.acId)); render(); return; }
    if (action === 'collapse') { expanded.clear(); render(); return; }
    const value = event.target?.dataset?.status; if (!value) return;
    enabledStatuses.has(value) ? enabledStatuses.delete(value) : enabledStatuses.add(value);
    filters.querySelectorAll('[data-status]').forEach(button => {
      const active = enabledStatuses.has(button.dataset.status); button.setAttribute('aria-pressed', String(active)); button.classList.toggle('selected', active);
    }); render();
  });
  document.getElementById('report').addEventListener('click', event => { selectCase(event.target); });
  document.getElementById('report').addEventListener('keydown', event => {
    if ((event.key === 'Enter' || event.key === ' ') && selectCase(event.target)) event.preventDefault();
  });
  document.getElementById('report').addEventListener('toggle', event => {
    const detail = event.target; if (!(detail instanceof HTMLDetailsElement) || !detail.dataset.acDetails) return;
    detail.open ? expanded.add(detail.dataset.acDetails) : expanded.delete(detail.dataset.acDetails);
  }, true);
  document.getElementById('query').addEventListener('input', render);
  render();
})();
