(() => {
  const data = JSON.parse(document.getElementById('topplecat-report-data').textContent);
  const review = String(data.schemaVersion || '').includes('review');
  const verification = String(data.schemaVersion || '').includes('verification');
  const e = value => String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character]);
  const pretty = value => e(JSON.stringify(value, null, 2));
  const id = value => String(value ?? '').replace(/[^A-Za-z0-9_-]/g, '-');
  const safeHref = raw => /^(https?:|mailto:|#)/i.test(String(raw || '')) ? String(raw) : '';
  const inline = source => {
    const escaped = e(source);
    return escaped
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\[([^\]]+)]\(([^\s)]+)(?:\s+&quot;[^&]*&quot;)?\)/g, (_all, label, href) => {
        const safe = safeHref(href.replace(/&amp;/g, '&'));
        return safe ? `<a href="${e(safe)}" target="_blank" rel="noopener">${label}</a>` : `${label} <code>${e(href)}</code>`;
      })
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>')
      .replace(/_([^_]+)_/g, '<em>$1</em>');
  };
  const badge = value => `<span class="badge ${e(value)}">${e(value)}</span>`;
  const phaseName = phase => ({ GIVEN: 'Given', WHEN: 'When', THEN: 'Then', AND: 'And' })[phase] || 'And';
  const parsedStep = step => {
    if (typeof step !== 'string') return step || {};
    const match = step.match(/^(Given|When|Then|And)\s+(.*)$/i);
    return match ? { phase: match[1].toUpperCase(), sentence: match[2] } : { phase: 'AND', sentence: step };
  };
  const scenario = (steps, live = false) => {
    const rows = (steps || []).map(parsedStep);
    if (!rows.length) return '<p class="meta">Scenario source is unavailable.</p>';
    let previous = '';
    return `<div class="scenario" aria-label="Given When Then scenario">${rows.map(step => {
      const phase = step.phase || 'AND'; const shown = previous === phase && phase !== 'AND' ? 'AND' : phase; previous = phase;
      const status = step.status ? ` ${e(step.status)}` : '';
      const duration = live && step.durationNanos ? ` <span class="meta">${(step.durationNanos / 1e6).toFixed(1)} ms</span>` : '';
      return `<div class="scenario-step${status}"><span class="bdd-keyword bdd-${e(shown.toLowerCase())}">${phaseName(shown)}</span><span class="bdd-sentence">${inline(String(step.sentence || '').replace(/^(Given|When|Then|And)\s+/i, ''))}${duration}</span></div>`;
    }).join('')}</div>`;
  };
  const flatten = (value, path = '') => {
    if (Array.isArray(value)) return value.length ? value.flatMap((item, index) => flatten(item, `${path}[${index}]`)) : [[path || 'value', '[]']];
    if (value && typeof value === 'object') return Object.keys(value).sort().length
      ? Object.keys(value).sort().flatMap(key => flatten(value[key], path ? `${path}.${key}` : key)) : [[path || 'value', '{}']];
    return [[path || 'value', typeof value === 'string' ? value : JSON.stringify(value)]];
  };
  const values = value => `<dl class="key-values">${flatten(value).map(([path, entry]) => `<div><dt>${e(path)}</dt><dd>${e(entry)}</dd></div>`).join('')}</dl>`;
  const javaKeywords = new Set(['abstract','assert','boolean','break','byte','case','catch','char','class','const','continue','default','do','double','else','enum','extends','final','finally','float','for','if','implements','import','instanceof','int','interface','long','native','new','package','private','protected','public','record','return','sealed','short','static','strictfp','super','switch','synchronized','this','throw','throws','transient','try','var','void','volatile','while','yield']);
  const escapedJavaString = value => {
    let html = ''; let literalStart = 0;
    for (let index = 0; index < value.length; index += 1) {
      if (value[index] !== '\\' || index + 1 >= value.length) continue;
      html += e(value.slice(literalStart, index));
      html += `<span class="tok-escape">${e(value.slice(index, index + 2))}</span>`;
      index += 1; literalStart = index + 1;
    }
    return `<span class="tok-string">${html}${e(value.slice(literalStart))}</span>`;
  };
  const tokenHtml = token => {
    if (token.type === 'plain') return e(token.text);
    if (token.type === 'string') return escapedJavaString(token.text);
    return `<span class="tok-${token.type}">${e(token.text)}</span>`;
  };
  const sourceTokens = (source, expression, type) => {
    const tokens = []; let position = 0; let match;
    while ((match = expression.exec(source)) !== null) {
      if (match.index > position) tokens.push({ type: 'plain', text: source.slice(position, match.index) });
      tokens.push({ type: typeof type === 'function' ? type(match[0]) : type, text: match[0] });
      position = match.index + match[0].length;
    }
    if (position < source.length) tokens.push({ type: 'plain', text: source.slice(position) });
    return tokens;
  };
  const javaTokens = source => {
    const tokens = []; let position = 0;
    const push = (type, text) => tokens.push({ type, text });
    while (position < source.length) {
      if (source.startsWith('//', position)) {
        const end = source.indexOf('\n', position); const next = end < 0 ? source.length : end;
        push('comment', source.slice(position, next)); position = next; continue;
      }
      if (source.startsWith('/*', position)) {
        const end = source.indexOf('*/', position + 2); const next = end < 0 ? source.length : end + 2;
        push('comment', source.slice(position, next)); position = next; continue;
      }
      if (source[position] === '"' || source[position] === "'") {
        const quote = source[position]; let next = position + 1;
        while (next < source.length) {
          if (source[next] === '\\') { next += 2; continue; }
          next += 1; if (source[next - 1] === quote) break;
        }
        push('string', source.slice(position, next)); position = next; continue;
      }
      const annotation = source.slice(position).match(/^@[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*/);
      if (annotation) { push('annotation', annotation[0]); position += annotation[0].length; continue; }
      const identifier = source.slice(position).match(/^[A-Za-z_$][\w$]*/);
      if (identifier) {
        const word = identifier[0];
        push(javaKeywords.has(word) ? 'keyword' : /^[A-Z]/.test(word) ? 'type' : 'plain', word);
        position += word.length; continue;
      }
      push('plain', source[position]); position += 1;
    }
    return tokens;
  };
  // A pure source-to-safe-HTML function: token recognition always precedes escaping and markup.
  const highlight = (language, source) => {
    const raw = String(source || ''); const lang = String(language || '').toLowerCase();
    if (lang === 'java') return javaTokens(raw).map(tokenHtml).join('');
    if (lang === 'json') return sourceTokens(raw, /"(?:\\.|[^"\\])*"(?=\s*:)|-?\b\d+(?:\.\d+)?\b/g, value => value.startsWith('"') ? 'json-key' : 'number').map(tokenHtml).join('');
    if (lang === 'yaml' || lang === 'yml') return sourceTokens(raw, /(^|\n)\s*[\w.-]+:/g, 'yaml-key').map(tokenHtml).join('');
    if (lang === 'markdown' || lang === 'md') return sourceTokens(raw, /(^|\n)(#{1,6}|[-*+] |\d+\. )/g, 'markdown').map(tokenHtml).join('');
    if (lang === 'mermaid') return `<span class="tok-mermaid">${e(raw)}</span>`;
    return e(raw);
  };
  const code = (language, source) => `<pre><code>${highlight(language, source)}</code></pre>`;
  const markdownBlock = block => {
    const anchor = block.anchorId ? ` id="ac-${id(block.anchorId)}"` : '';
    switch (block.kind) {
      case 'HEADING': { const level = Math.min(Math.max((block.headingLevel || 1) + 1, 2), 6); return `<h${level}${anchor}>${inline(block.text)}</h${level}>`; }
      case 'PARAGRAPH': return `<p${anchor}>${inline(block.text)}</p>`;
      case 'LIST': return `<ul${anchor}>${(block.items || []).map(item => `<li>${inline(item)}</li>`).join('')}</ul>`;
      case 'ORDERED_LIST': return `<ol${anchor}>${(block.items || []).map(item => `<li>${inline(item)}</li>`).join('')}</ol>`;
      case 'TASK_LIST': return `<ul class="task-list"${anchor}>${(block.items || []).map(item => { const checked = /^\[x]/i.test(item); return `<li><input type="checkbox" disabled ${checked ? 'checked' : ''}>${inline(item.replace(/^\[[ xX]]\s*/, ''))}</li>`; }).join('')}</ul>`;
      case 'BLOCK_QUOTE': return `<blockquote${anchor}>${String(block.text || '').split('\n').map(line => `<p>${inline(line)}</p>`).join('')}</blockquote>`;
      case 'HORIZONTAL_RULE': return '<hr>';
      case 'TABLE': return `<div class="table-wrap"${anchor}><table><thead><tr>${(block.tableHeaders || []).map(head => `<th>${inline(head)}</th>`).join('')}</tr></thead><tbody>${(block.tableRows || []).map(row => `<tr>${row.map(cell => `<td>${inline(cell)}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`;
      case 'IMAGE': {
        const destination = String(block.destination || ''); const source = /^assets\/spec\/[a-f0-9]{64}\.[a-z0-9]+$/.test(destination);
        if (source) return `<figure${anchor}><img src="${e(destination)}" alt="${e(block.text)}"${block.title ? ` title="${e(block.title)}"` : ''}><figcaption>${inline(block.text)}</figcaption></figure>`;
        const safe = safeHref(destination); const link = safe ? ` <a href="${e(safe)}" target="_blank" rel="noopener">Open remote image</a>` : '';
        return `<div class="image-placeholder"${anchor}><strong>Image unavailable.</strong> ${inline(block.text || 'No alternative text was authored.')} ${e(block.title || '')}${link}</div>`;
      }
      case 'MERMAID': return `<section class="mermaid-panel"${anchor}><div class="mermaid-diagram"><div class="mermaid-source" hidden>${e(block.text)}</div></div><details><summary>View Mermaid source</summary>${code('mermaid', block.text)}</details></section>`;
      case 'CODE_FENCE': return `<section${anchor}>${code(block.language, block.text)}</section>`;
      default: return `<section${anchor}><p class="meta">Content could not be rendered as Markdown. Its escaped source is preserved below.</p>${code(block.language || 'markdown', block.text)}</section>`;
    }
  };
  const documentView = document => `<article class="document" id="document-${id(document.path)}"><p class="document-identity"><code>${e(document.path)}</code></p>${(document.blocks || []).map(markdownBlock).join('')}</article>`;
  const visibility = value => value === 'HIDDEN' ? '<span class="badge HIDDEN">Reviewer case</span>' : '<span class="badge PUBLIC">Public case</span>';
  const method = item => item?.sourceCode ? `<details><summary>Acceptance Method source</summary><p class="meta">Only the AC-bound acceptance method is shown. Stage, helper, and production source are excluded.</p>${item.methodIdentity ? `<p class="technical-meta"><code>${e(item.methodIdentity)}</code>${item.sourceFile ? `, ${e(item.sourceFile)}:${e(item.sourceLine)}` : ''}</p>` : ''}${code('java', item.sourceCode)}</details>` : '<p class="meta">Acceptance Method source is unavailable.</p>';
  const reviewCase = item => `<article class="case-card"><p>${visibility(item.visibility)} <strong>${e(item.caseId)}</strong></p>${scenario(item.scenario?.length ? item.scenario : [], false)}<div class="case-grid"><section><h4>Inputs</h4>${values(item.inputs)}</section><section><h4>Expected result</h4>${values(item.expected)}</section></div></article>`;
  const reviewProperties = properties => !(properties || []).length ? '' : `<section><h4>Property declarations</h4>${properties.map(property => `<article class="case-card"><strong>${e(property.title)}</strong><p class="meta"><code>${e(property.methodIdentity)}</code>. ${e(property.tries)} tries, at most ${e(property.maxDiscards)} discards and ${e(property.maxShrinks)} shrinks.</p><details><summary>Property source and technical details</summary><p class="technical-meta">${e(property.sourceFile)}:${e(property.sourceLine)}</p>${code('java', property.sourceCode)}</details></article>`).join('')}</section>`;
  const advisories = () => !(data.contractQualityAdvisories || []).length ? '' : `<section class="report-section" id="contract-quality-advisories"><h2>Contract Quality Advisories</h2><p>These reviewer-only observations are non-blocking. They do not add a business rule, change an execution result, or alter a Gate.</p>${data.contractQualityAdvisories.map(advisory => `<div class="advisory"><p><strong>${e(advisory.ruleCode)}</strong> for <a href="#ac-${id(advisory.acId)}">${e(advisory.acId)}</a></p><p>${e(advisory.expectedPath)}. Public rows: ${e(advisory.publicCount)}. Reviewer rows: ${e(advisory.hiddenCount)}.</p></div>`).join('')}</section>`;
  const reviewAc = item => `<article class="ac-review" id="review-${id(item.acId)}"><div class="ac-heading"><span class="ac-id">${e(item.acId)}</span><h3>${e(item.title)}</h3></div>${item.location?.documentPath ? `<p class="meta">Selected SDD: <code>${e(item.location.documentPath)}</code>, document position ${e(item.location.documentPosition)}.</p>` : '<p class="meta">No external Spec document was selected for this full-contract review.</p>'}<h4>Executable Scenario and typed cases</h4>${(item.cases || []).map(reviewCase).join('') || '<p class="meta">No typed case rows were recorded.</p>'}${reviewProperties(item.properties)}${method(item.method)}<details><summary>Technical and policy metadata</summary><p class="technical-meta">This report projects the checked executable contract. Its details do not add a rule or execution result.</p></details></article>`;
  const reviewPage = () => {
    document.title = 'Spec Review'; document.getElementById('title').textContent = 'Spec Review'; document.getElementById('notice').textContent = 'Specification prepared — not executed';
    const docs = data.selectedSpecDocuments || [];
    const docIntro = docs.length ? 'The complete selected SDD documents appear first. The executable material below is the Java/JUnit contract bound to their ACs.' : 'No external Spec document was selected. This is the complete executable contract, not an invented Markdown document.';
    document.getElementById('summary').innerHTML = `<section class="report-intro"><h2>Specification prepared — not executed</h2><p>${docIntro}</p></section>`;
    document.getElementById('report').innerHTML = `${docs.length ? `<section class="report-section" id="selected-documents"><h2>Selected SDD documents</h2>${docs.map(documentView).join('')}</section>` : ''}<section class="report-section" id="executable-material"><h2>Executable acceptance material</h2>${(data.acceptanceConditions || []).map(reviewAc).join('')}</section>${advisories()}`;
    document.getElementById('outline').innerHTML = `<h2>On this page</h2>${docs.map(doc => `<a href="#document-${id(doc.path)}">${e(doc.path)}</a>`).join('')}<a href="#executable-material">Executable acceptance material</a>${(data.acceptanceConditions || []).map(ac => `<a href="#review-${id(ac.acId)}">${e(ac.acId)}</a>`).join('')}${(data.contractQualityAdvisories || []).length ? '<a href="#contract-quality-advisories">Advisories</a>' : ''}`;
  };
  const gate = (name, heading) => (data.gates || []).find(item => item.name === name) || { name, verdict: 'INCOMPLETE', reason: 'Current-run evidence is unavailable.' };
  const gateCard = item => `<div class="gate-card ${e(item.verdict)}"><p>${badge(item.verdict)} <strong>${e(item.name)}</strong></p>${item.reason ? `<p>${e(item.reason)}</p>` : ''}</div>`;
  const failedSteps = item => (item.steps || []).filter(step => step.status === 'FAIL');
  const comparison = item => {
    const step = failedSteps(item).find(candidate => (candidate.comparisons || []).length) || (item.steps || []).find(candidate => (candidate.comparisons || []).length);
    const comparisons = step?.comparisons || [];
    if (!comparisons.length) return '';
    return `<section class="comparison"><h4>Field-level expected and actual comparison</h4><p class="meta">Bound to compiler Step <code>${e(step.stepId)}</code>.</p>${comparisons.map(entry => `<h5>Expected <code>${e(entry.expectedKey)}</code></h5><div class="table-wrap"><table><thead><tr><th>Path</th><th>Difference</th><th>Expected</th><th>Actual</th></tr></thead><tbody>${(entry.differences || []).map(diff => `<tr><td>${e(diff.path)}</td><td>${e(diff.kind)}</td><td>${e(JSON.stringify(diff.expected))}</td><td>${e(JSON.stringify(diff.actual))}</td></tr>`).join('')}</tbody></table></div>`).join('')}</section>`;
  };
  const stepData = item => {
    const recorded = (item.steps || []).filter(step => (step.actualArguments || []).length);
    if (!recorded.length) return '';
    return `<section class="step-data"><h4>Step data</h4>${recorded.map(step => `<details><summary>Arguments for <code>${e(step.stepId)}</code></summary>${values(step.actualArguments)}</details>`).join('')}</section>`;
  };
  const lazyCases = new Map();
  const verificationCaseContent = (ac, item) => `<h4>Public rule being checked</h4><p><a href="#verification-${id(ac.acId)}">${e(ac.acId)}: ${e(ac.title)}</a></p><h4>Scenario</h4>${scenario(item.steps?.length ? item.steps.map(step => ({ ...step, phase: ac.stepPhases?.[step.stepId] || 'AND' })) : ac.scenario, Boolean(item.steps?.length))}${stepData(item)}<h4>Failed or last reached Step</h4>${(() => { const last = failedSteps(item)[0] || (item.steps || []).filter(step => step.status !== 'SKIPPED').at(-1); return last ? `<p><code>${e(last.stepId)}</code> ${e(last.sentence)}</p>` : '<p class="meta">No Scenario Step was reached.</p>'; })()}${comparison(item)}<div class="case-grid"><section><h4>Inputs</h4>${values(item.inputs)}</section><section><h4>Complete expected result</h4>${values(item.expected)}</section></div><details class="raw-failure"><summary>Raw failure and technical metadata</summary>${item.failure ? `<pre>${e(item.failure)}</pre>` : '<p class="meta">No raw failure was recorded.</p>'}<h5>Expected consumption</h5>${values(item.expectedConsumption || {})}</details>`;
  const verificationCase = (ac, item, open) => {
    const key = JSON.stringify([ac.acId, item.caseId]);
    lazyCases.set(key, { ac, item });
    return `<details class="case-card" data-case-id="${e(item.caseId)}" data-case-status="${e(item.status)}" data-search="${e(`${ac.acId} ${ac.title} ${item.caseId}`.toLowerCase())}" data-lazy-case="${e(key)}"${open ? ' open' : ''} id="case-${id(item.caseId)}"><summary>${visibility(item.visibility)} <strong>${e(item.caseId)}</strong> ${badge(item.status)}</summary><div class="lazy-case-content">${open ? verificationCaseContent(ac, item) : ''}</div></details>`;
  };
  const acCases = (visibilityName, openFailure) => (data.acceptanceConditions || []).flatMap(ac => (ac.cases || []).filter(item => item.visibility === visibilityName).map(item => ({ ac, item }))).map(({ ac, item }, index) => verificationCase(ac, item, openFailure && index === 0 && item.status === 'FAIL')).join('') || '<p class="meta">No current rows were available for this area.</p>';
  const integrityFailed = () => gate('CONTRACT_INTEGRITY').verdict !== 'PASS';
  const propertyResults = () => (data.acceptanceConditions || []).flatMap(ac => (ac.properties || []).map(property => `<article class="case-card" id="property-${id(property.methodIdentity)}"><h3>${e(property.title)} ${badge(property.status)}</h3><p class="meta"><code>${e(property.methodIdentity)}</code>. ${e(property.completedTrials)}/${e(property.requestedTrials)} trials; ${e(property.discards)} discards.</p>${property.incompleteReason ? `<p>${e(property.incompleteReason)}</p>` : ''}${property.originalCounterexample ? `<h4>Original counterexample</h4>${code('json', property.originalCounterexample.choicesJson)}` : ''}${property.shrunkCounterexample ? `<h4>Shrunk counterexample</h4>${code('json', property.shrunkCounterexample.choicesJson)}` : ''}${property.replayToken ? `<p class="meta">Replay token <code>${e(property.replayToken)}</code></p>` : ''}</article>`)).join('') || '<p class="meta">No Property declaration applied to this scope.</p>';
  const mutation = () => {
    const value = data.mutationAttribution; const gateValue = gate('MUTATION');
    if (!value) return `${gateCard(gateValue)}<p class="meta">No current managed PIT attribution was available.</p>`;
    const outcome = rows => !rows?.length ? '' : `<div class="table-wrap"><table><thead><tr><th>PIT status</th><th>Detected</th><th>Mutants</th></tr></thead><tbody>${rows.map(row => `<tr><td><code>${e(row.status)}</code></td><td>${e(row.detected)}</td><td>${e(row.count)}</td></tr>`).join('')}</tbody></table></div>`;
    const detected = (value.mutations || []).filter(item => item.detected).length;
    return `${gateCard(gateValue)}<p>PIT <code>${e(value.pitVersion)}</code>, managed profile <code>${e(value.managedProfileId)}</code>.</p><h3>PIT global outcome</h3><p>${e(detected)}/${e(value.producerMutationCount)} mutants were detected by at least one test.</p><p class="meta">This PIT-wide observation does not mean that every Acceptance Method detected every mutant.</p><p>${e(value.producerMutationCount)} producer mutants. ${e(value.uniquelyAttributedMutationCount)} uniquely attributed. ${e(value.unattributedMutationCount)} unattributed.</p><h3>Managed operator IDs</h3><ul>${(value.managedOperatorIds || []).map(operator => `<li><code>${e(operator)}</code></li>`).join('')}</ul><h3>Raw producer outcomes</h3>${outcome(value.producerOutcomeCounts)}<h3>Per-AC Acceptance Method detection</h3><p class="meta">The Mutation Gate uses each Acceptance Method's covered-mutant detection rate against its sealed threshold. It does not blend this rate with PIT's global outcome.</p><div class="table-wrap"><table><thead><tr><th>AC</th><th>Covered</th><th>Detected by Acceptance Method</th><th>Threshold</th><th>Detection rate</th></tr></thead><tbody>${(value.assessments || []).map(assessment => `<tr><td>${e(assessment.acId)}</td><td>${e(assessment.coveredMutantCount)}</td><td>${e(assessment.killedByAcceptanceMethodMutantCount)}</td><td>${e(assessment.sealedThreshold)}%</td><td>${e(assessment.detectionRate)}%</td></tr>`).join('')}</tbody></table></div>${(value.assessments || []).filter(assessment => assessment.attributionGap).map(assessment => `<p class="meta">${e(assessment.acId)} has no managed-profile attribution evidence in this run. Reviewer judgment is required.</p>`).join('')}<details><summary>Raw PIT findings</summary>${(value.mutations || []).map((item, index) => `<article class="case-card"><h4>Mutant ${index + 1}: <code>${e(item.status)}</code></h4><p>${e(item.description)}</p><p class="meta">Mutator <code>${e(item.mutator)}</code>. Detected: ${e(item.detected)}.</p><h5>coveringTests</h5><ul>${(item.coveringTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul><h5>killingTests</h5><ul>${(item.killingTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul><h5>succeedingTests</h5><ul>${(item.succeedingTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul></article>`).join('')}</details>`;
  };
  const problems = () => {
    if (data.verdict === 'PASS') return '';
    const gates = integrityFailed()
      ? [gate('CONTRACT_INTEGRITY')]
      : (data.gates || []).filter(item => item.verdict === 'FAIL').concat((data.gates || []).filter(item => item.verdict === 'INCOMPLETE'));
    const caseProblems = integrityFailed() ? [] : (data.acceptanceConditions || []).flatMap(ac => (ac.cases || []).filter(item => item.status === 'FAIL').map(item => ({ ac, item })));
    const all = gates.map(item => `<li><a href="#${item.name === 'CONTRACT_INTEGRITY' ? 'contract-integrity' : item.name === 'JUNIT' || item.name === 'EXPECTED_CONSUMPTION' ? 'public-acceptance' : item.name === 'REVIEWER_JUNIT' ? 'hidden-tests' : item.name === 'PROPERTY' ? 'property-testing' : 'mutation-testing'}">${e(item.name)}</a>: ${e(item.reason || `This Gate recorded ${item.verdict} in the current run.`)}</li>`).concat(caseProblems.map(({ ac, item }) => `<li><a href="#case-${id(item.caseId)}">${e(ac.acId)} / ${e(item.caseId)}</a>: this ${item.visibility === 'HIDDEN' ? 'reviewer' : 'public'} typed row failed while executing its recorded Scenario.</li>`));
    return `<section class="problem-summary" id="problems"><h2>Problems Summary</h2><ol>${all.join('') || '<li>Verification is incomplete because current-run evidence is unavailable.</li>'}</ol></section>`;
  };
  const verificationPage = () => {
    document.title = 'Verification Report'; document.getElementById('title').textContent = 'Verification Report';
    const conclusion = data.verdict === 'PASS' ? 'Delivery accepted — verification passed' : data.verdict === 'FAIL' ? 'Delivery rejected — verification failed' : 'Verification incomplete';
    document.getElementById('notice').textContent = conclusion;
    const run = data.run || {}; const scope = data.deliveryScope || {}; const selected = (scope.acceptanceConditionIds || []).join(', ') || 'Full executable contract';
    document.getElementById('summary').innerHTML = `<section class="report-intro verification ${e(data.verdict)}"><h2>${e(conclusion)}</h2><p>Aggregate verdict: ${badge(data.verdict)}. Failed Gates: ${e(run.failedGateCount ?? 0)}. Incomplete Gates: ${e(run.incompleteGateCount ?? 0)}. Failed ACs: ${e(run.failedAcceptanceConditionCount ?? 0)}. Failed cases: ${e(run.failedCaseCount ?? 0)}.</p><p class="meta">Run ID: <code>${e(run.runId || 'unavailable')}</code>. Started: ${e(run.startedAt || 'unavailable')}. Finished: ${e(run.finishedAt || data.generatedAt || 'unavailable')}.</p><p class="meta">Selected and executed scope: ${e(selected)}. Hidden rows: ${e(scope.executedHiddenRows ?? 0)}. Properties: ${e(scope.executedPublicProperties ?? 0)}.</p></section><section class="filter-controls" aria-label="Verification report filters"><label>Find AC or case <input id="case-query" type="search" autocomplete="off"></label>${['FAIL','PASS','NOT_REPORTED'].map(status => `<button type="button" data-status-filter="${status}" aria-pressed="false">${status}</button>`).join('')}</section>${problems()}`;
    const blocked = '<p class="suppressed">Not executed because Contract Integrity did not establish a trusted contract for this run.</p>';
    document.getElementById('report').innerHTML = `<section class="report-section" id="contract-integrity"><h2>Contract Integrity</h2>${gateCard(gate('CONTRACT_INTEGRITY'))}</section><section class="report-section" id="public-acceptance"><h2>Public Acceptance</h2>${integrityFailed() ? blocked : `${gateCard(gate('JUNIT'))}${gateCard(gate('EXPECTED_CONSUMPTION'))}${acCases('PUBLIC', true)}`}</section><section class="report-section" id="hidden-tests"><h2>Hidden Tests</h2>${integrityFailed() ? blocked : `${gateCard(gate('REVIEWER_JUNIT'))}${acCases('HIDDEN', true)}`}</section><section class="report-section" id="property-testing"><h2>Property-Based Testing</h2>${integrityFailed() ? blocked : `${gateCard(gate('PROPERTY'))}${propertyResults()}`}</section><section class="report-section" id="mutation-testing"><h2>Mutation Testing</h2>${integrityFailed() ? blocked : mutation()}</section>`;
    document.getElementById('outline').innerHTML = '<h2>Verification</h2><a href="#problems">Problems Summary</a><a href="#contract-integrity">Contract Integrity</a><a href="#public-acceptance">Public Acceptance</a><a href="#hidden-tests">Hidden Tests</a><a href="#property-testing">Property-Based Testing</a><a href="#mutation-testing">Mutation Testing</a>';
  };
  if (!review && !verification) throw new Error('Unsupported ToppleCat report projection.');
  review ? reviewPage() : verificationPage();
  if (verification) {
    const statuses = new Set(); const query = document.getElementById('case-query');
    const applyCaseFilters = () => document.querySelectorAll('[data-case-id]').forEach(item => {
      const queryMatch = !query.value.trim() || item.dataset.search.includes(query.value.trim().toLowerCase());
      const statusMatch = !statuses.size || statuses.has(item.dataset.caseStatus);
      item.hidden = !(queryMatch && statusMatch);
    });
    query.addEventListener('input', applyCaseFilters);
    document.querySelectorAll('[data-status-filter]').forEach(button => button.addEventListener('click', () => {
      const status = button.dataset.statusFilter; statuses.has(status) ? statuses.delete(status) : statuses.add(status);
      button.setAttribute('aria-pressed', String(statuses.has(status))); applyCaseFilters();
    }));
    document.querySelectorAll('details[data-lazy-case]').forEach(details => details.addEventListener('toggle', () => {
      if (!details.open || details.dataset.loaded === 'true') return;
      const record = lazyCases.get(details.dataset.lazyCase);
      if (!record) return;
      details.querySelector('.lazy-case-content').innerHTML = verificationCaseContent(record.ac, record.item);
      details.dataset.loaded = 'true';
    }));
  }
  document.querySelectorAll('.mermaid-diagram').forEach(container => {
    const source = container.querySelector('.mermaid-source')?.textContent || '';
    try { container.innerHTML = window.ToppleCatMermaid.render(source); } catch (_error) { container.innerHTML = '<p class="mermaid-error">Diagram could not be rendered. The escaped original Mermaid source is available below.</p>'; }
  });
})();
