(() => {
  const data = JSON.parse(document.getElementById('topplecat-report-data').textContent);
  const presentation = JSON.parse(document.getElementById('topplecat-report-presentation').textContent);
  const catalogs = {
    en: {
      'phase.GIVEN': 'Given', 'phase.WHEN': 'When', 'phase.THEN': 'Then', 'phase.AND': 'And',
      'scenario.unavailable': 'Scenario source is unavailable.', 'scenario.aria': 'Given When Then scenario',
      'image.openRemote': 'Open remote image', 'image.unavailable': 'Image unavailable.', 'image.noAlt': 'No alternative text was authored.',
      'mermaid.source': 'View Mermaid source', 'mermaid.error': 'Diagram could not be rendered. The escaped original Mermaid source is available below.',
      'markdown.unrenderable': 'Content could not be rendered as Markdown. Its escaped source is preserved below.',
      'visibility.hidden': 'Reviewer case', 'visibility.public': 'Public case',
      'method.source': 'Acceptance Method source', 'method.description': 'Only the AC-bound acceptance method is shown. Stage, helper, and production source are excluded.', 'method.unavailable': 'Acceptance Method source is unavailable.',
      'inputs': 'Inputs', 'expectedResult': 'Expected result',
      'property.declarations': 'Property declarations', 'property.tries': '{0} tries, at most {1} discards and {2} shrinks.', 'property.details': 'Property source and technical details',
      'advisory.heading': 'Contract Quality Advisories', 'advisory.description': 'These reviewer-only observations are non-blocking. They do not add a business rule, change an execution result, or alter a Gate.', 'advisory.for': 'for', 'advisory.publicRows': 'Public rows: {0}.', 'advisory.reviewerRows': 'Reviewer rows: {0}.',
      'review.selectedSdd': 'Selected SDD', 'review.documentPosition': 'document position {0}.', 'review.noSpec': 'No external Spec document was selected for this full-contract review.', 'review.material': 'Executable Scenario and typed cases', 'review.noRows': 'No typed case rows were recorded.', 'review.techSummary': 'Technical and policy metadata', 'review.techMeta': 'This report projects the checked executable contract. Its details do not add a rule or execution result.',
      'review.title': 'Spec Review', 'review.notice': 'Specification prepared — not executed', 'review.withDocs': 'The complete selected SDD documents appear first. The executable material below is the Java/JUnit contract bound to their ACs.', 'review.withoutDocs': 'No external Spec document was selected. This is the complete executable contract, not an invented Markdown document.', 'review.selectedDocuments': 'Selected SDD documents', 'review.outline': 'On this page', 'review.advisories': 'Advisories',
      'gate.evidenceUnavailable': 'Current-run evidence is unavailable.', 'gate.canonicalReason': '{0}',
      'comparison.heading': 'Field-level expected and actual comparison', 'comparison.boundTo': 'Bound to compiler Step', 'comparison.expected': 'Expected', 'comparison.path': 'Path', 'comparison.difference': 'Difference', 'comparison.actual': 'Actual',
      'stepData.heading': 'Step data', 'stepData.arguments': 'Arguments for',
      'case.publicRule': 'Public rule being checked', 'case.scenario': 'Scenario', 'case.failedStep': 'Failed or last reached Step', 'case.noStep': 'No Scenario Step was reached.', 'case.completeExpected': 'Complete expected result', 'case.rawFailure': 'Raw failure and technical metadata', 'case.noRawFailure': 'No raw failure was recorded.', 'case.expectedConsumption': 'Expected consumption', 'case.noCurrentRows': 'No current rows were available for this area.',
      'property.none': 'No Property declaration applied to this scope.', 'property.results': '{0} trials; {1} discards.', 'property.original': 'Original counterexample', 'property.shrunk': 'Shrunk counterexample', 'property.replay': 'Replay token',
      'acResults.publicAcceptance': 'Public Acceptance', 'acResults.hiddenTests': 'Hidden Tests', 'acResults.mutationTesting': 'Mutation Testing', 'acResults.passed': 'Passed', 'acResults.failed': 'Did not pass', 'acResults.noData': 'No data', 'acResults.recordedReason': 'No data. The current run recorded: {0}',
      'mutation.intro': "During verification, ToppleCat temporarily changes production behavior and checks it again with this AC's public acceptance work.", 'mutation.meetsResult': 'Meets requirement', 'mutation.belowResult': 'Below requirement', 'mutation.meets': '{0} relevant changes; public acceptance noticed {1}; meets the sealed {2}% requirement.', 'mutation.below': 'Of {0} relevant changes, public acceptance noticed {1}, below the sealed {2}% requirement.', 'mutation.noData': 'No data. This run has no Mutation result to show for this AC.', 'mutation.technicalDetails': 'Mutation technical details', 'mutation.pitStatus': 'PIT status', 'mutation.detected': 'Detected', 'mutation.mutants': 'Mutants', 'mutation.profile': 'PIT {0}, managed profile {1}.', 'mutation.global': 'PIT global outcome', 'mutation.detectedCount': '{0}/{1} mutants were detected by at least one test.', 'mutation.globalMeaning': 'This PIT-wide observation does not mean that every Acceptance Method detected every mutant.', 'mutation.producerCounts': '{0} producer mutants. {1} uniquely attributed. {2} unattributed.', 'mutation.operatorIds': 'Managed operator IDs', 'mutation.rawOutcomes': 'Raw producer outcomes', 'mutation.perAc': 'Per-AC Acceptance Method detection', 'mutation.perAcMeaning': "The Mutation Gate uses each Acceptance Method's covered-mutant detection rate against its sealed threshold. It does not blend this rate with PIT's global outcome.", 'mutation.ac': 'AC', 'mutation.covered': 'Covered', 'mutation.detectedByMethod': 'Detected by Acceptance Method', 'mutation.threshold': 'Threshold', 'mutation.rate': 'Detection rate', 'mutation.rawFindings': 'Raw PIT findings', 'mutation.mutant': 'Mutant {0}', 'mutation.mutator': 'Mutator {0}. Detected: {1}.', 'mutation.coveringTests': 'coveringTests', 'mutation.killingTests': 'killingTests', 'mutation.succeedingTests': 'succeedingTests',
      'problems.defaultGate': 'This Gate recorded {0} in the current run.', 'problems.hiddenCase': 'this reviewer typed row failed while executing its recorded Scenario.', 'problems.publicCase': 'this public typed row failed while executing its recorded Scenario.', 'problems.heading': 'Problems Summary', 'problems.noEvidence': 'Verification is incomplete because current-run evidence is unavailable.',
      'verification.title': 'Verification Report', 'verification.pass': 'Delivery accepted — verification passed', 'verification.fail': 'Delivery rejected — verification failed', 'verification.incomplete': 'Verification incomplete', 'verification.aggregate': 'Aggregate verdict: {0}. Failed Gates: {1}. Incomplete Gates: {2}. Failed ACs: {3}. Failed cases: {4}.', 'verification.unavailable': 'unavailable', 'verification.run': 'Run ID: {0}. Started: {1}. Finished: {2}.', 'verification.fullContract': 'Full executable contract', 'verification.scope': 'Selected and executed scope: {0}. Hidden rows: {1}. Properties: {2}.', 'verification.filters': 'Verification report filters', 'verification.find': 'Find AC or case', 'verification.blocked': 'Not executed because Contract Integrity did not establish a trusted contract for this run.', 'verification.contractIntegrity': 'Contract Integrity', 'verification.publicAcceptance': 'Public Acceptance', 'verification.hiddenTests': 'Hidden Tests', 'verification.propertyTesting': 'Property-Based Testing', 'verification.mutationTesting': 'Mutation Testing', 'verification.outline': 'Verification',
      'shell.skipLink': 'Skip to report content', 'shell.outline': 'Report outline'
    },
    'zh-TW': {
      'phase.GIVEN': '假設', 'phase.WHEN': '當', 'phase.THEN': '那麼', 'phase.AND': '且',
      'scenario.unavailable': '情境來源無法使用。', 'scenario.aria': '假設、當、那麼情境',
      'image.openRemote': '開啟遠端圖片', 'image.unavailable': '圖片無法使用。', 'image.noAlt': '沒有撰寫替代文字。',
      'mermaid.source': '查看 Mermaid 原始碼', 'mermaid.error': '無法繪製圖表。下方保留了已跳脫的原始 Mermaid 原始碼。',
      'markdown.unrenderable': '無法將內容算繪為 Markdown。下方保留了已跳脫的原始內容。',
      'visibility.hidden': '審閱者案例', 'visibility.public': '公開案例',
      'method.source': '驗收方法原始碼', 'method.description': '此處只顯示綁定 AC 的驗收方法；不會顯示 Stage、輔助程式或產品原始碼。', 'method.unavailable': '驗收方法原始碼無法使用。',
      'inputs': '輸入', 'expectedResult': '預期結果',
      'property.declarations': 'Property 宣告', 'property.tries': '{0} 次嘗試，最多 {1} 次捨棄與 {2} 次縮減。', 'property.details': 'Property 原始碼與技術細節',
      'advisory.heading': '契約品質提醒', 'advisory.description': '這些僅供審閱者閱讀的觀察不會阻擋流程。它們不會新增業務規則、改變執行結果或修改 Gate。', 'advisory.for': '適用於', 'advisory.publicRows': '公開資料列：{0}。', 'advisory.reviewerRows': '審閱者資料列：{0}。',
      'review.selectedSdd': '已選 SDD', 'review.documentPosition': '文件位置 {0}。', 'review.noSpec': '這次完整契約審閱沒有選擇外部 Spec 文件。', 'review.material': '可執行的情境與型別案例', 'review.noRows': '沒有記錄型別案例資料列。', 'review.techSummary': '技術與政策中繼資料', 'review.techMeta': '本報告投影已檢查的可執行契約；其中細節不會新增規則或執行結果。',
      'review.title': '規格審閱', 'review.notice': '規格已備妥，尚未執行', 'review.withDocs': '先列出完整的已選 SDD 文件，接著才是綁定其 AC 的 Java/JUnit 可執行材料。', 'review.withoutDocs': '沒有選擇外部 Spec 文件。這是完整的可執行契約，不是 ToppleCat 臆造的 Markdown 文件。', 'review.selectedDocuments': '已選 SDD 文件', 'review.outline': '本頁內容', 'review.advisories': '提醒',
      'gate.evidenceUnavailable': '本次執行證據無法使用。', 'gate.canonicalReason': '本次執行證據記錄的原始原因：{0}',
      'comparison.heading': '欄位層級的預期值與實際值比較', 'comparison.boundTo': '綁定至編譯器步驟', 'comparison.expected': '預期', 'comparison.path': '路徑', 'comparison.difference': '差異', 'comparison.actual': '實際',
      'stepData.heading': '步驟資料', 'stepData.arguments': '下列步驟的引數：',
      'case.publicRule': '正在檢查的公開規則', 'case.scenario': '情境', 'case.failedStep': '失敗或最後到達的步驟', 'case.noStep': '沒有到達任何情境步驟。', 'case.completeExpected': '完整預期結果', 'case.rawFailure': '原始失敗與技術中繼資料', 'case.noRawFailure': '沒有記錄原始失敗。', 'case.expectedConsumption': '預期值使用狀態', 'case.noCurrentRows': '此區域沒有可用的本次資料列。',
      'property.none': '此範圍沒有適用的 Property 宣告。', 'property.results': '{0} 次嘗試；{1} 次捨棄。', 'property.original': '原始反例', 'property.shrunk': '縮減後反例', 'property.replay': '重播權杖',
      'acResults.publicAcceptance': '公開驗收', 'acResults.hiddenTests': '隱藏測試', 'acResults.mutationTesting': '突變測試', 'acResults.passed': '通過', 'acResults.failed': '未通過', 'acResults.noData': '無資料', 'acResults.recordedReason': '無資料。本次執行記錄：{0}',
      'mutation.intro': '驗證時，ToppleCat 暫時改變正式程式的行為，再用這個 AC 的公開驗收檢查。', 'mutation.meetsResult': '符合要求', 'mutation.belowResult': '低於要求', 'mutation.meets': '{0} 個相關改動，公開驗收發現 {1} 個，符合封存的 {2}% 要求。', 'mutation.below': '{0} 個相關改動，公開驗收只發現 {1} 個，低於封存的 {2}% 要求。', 'mutation.noData': '無資料。本次執行沒有此 AC 可顯示的突變測試結果。', 'mutation.technicalDetails': '突變測試技術細節', 'mutation.pitStatus': 'PIT 狀態', 'mutation.detected': '已偵測', 'mutation.mutants': '突變體', 'mutation.profile': 'PIT {0}，受管理設定檔 {1}。', 'mutation.global': 'PIT 全域結果', 'mutation.detectedCount': '{0}/{1} 個突變體至少被一項測試偵測到。', 'mutation.globalMeaning': '這個 PIT 全域觀察不代表每個驗收方法都偵測到每個突變體。', 'mutation.producerCounts': '{0} 個 producer 突變體；{1} 個獲得唯一歸因；{2} 個未歸因。', 'mutation.operatorIds': '受管理運算子 ID', 'mutation.rawOutcomes': '原始 producer 結果', 'mutation.perAc': '每個 AC 的驗收方法偵測結果', 'mutation.perAcMeaning': 'Mutation Gate 會以每個驗收方法對已涵蓋突變體的偵測率，比對其封印的門檻；不會與 PIT 全域結果混合。', 'mutation.ac': 'AC', 'mutation.covered': '已涵蓋', 'mutation.detectedByMethod': '由驗收方法偵測', 'mutation.threshold': '門檻', 'mutation.rate': '偵測率', 'mutation.rawFindings': '原始 PIT 發現', 'mutation.mutant': '突變體 {0}', 'mutation.mutator': 'Mutator {0}。已偵測：{1}。', 'mutation.coveringTests': 'coveringTests', 'mutation.killingTests': 'killingTests', 'mutation.succeedingTests': 'succeedingTests',
      'problems.defaultGate': '這個 Gate 在這次執行記錄為 {0}。', 'problems.hiddenCase': '這個審閱者型別資料列在執行所記錄的情境時失敗。', 'problems.publicCase': '這個公開型別資料列在執行所記錄的情境時失敗。', 'problems.heading': '問題摘要', 'problems.noEvidence': '驗證不完整，因為本次執行證據無法使用。',
      'verification.title': '驗證報告', 'verification.pass': '交付已接受，驗證通過', 'verification.fail': '交付遭拒，驗證失敗', 'verification.incomplete': '驗證不完整', 'verification.aggregate': '整體 verdict：{0}。失敗 Gate：{1}。不完整 Gate：{2}。失敗 AC：{3}。失敗案例：{4}。', 'verification.unavailable': '無法使用', 'verification.run': '執行 ID：{0}。開始：{1}。完成：{2}。', 'verification.fullContract': '完整可執行契約', 'verification.scope': '已選並執行的範圍：{0}。隱藏資料列：{1}。Property：{2}。', 'verification.filters': '驗證報告篩選器', 'verification.find': '尋找 AC 或案例', 'verification.blocked': 'Contract Integrity 未能為本次執行建立可信的契約，因此沒有執行。', 'verification.contractIntegrity': '契約完整性', 'verification.publicAcceptance': '公開驗收', 'verification.hiddenTests': '隱藏測試', 'verification.propertyTesting': '性質導向測試', 'verification.mutationTesting': '突變測試', 'verification.outline': '驗證',
      'shell.skipLink': '跳至報告內容', 'shell.outline': '報告大綱'
    }
  };
  const language = String(presentation.language || '');
  const messages = catalogs[language];
  if (!messages) throw new Error('Unsupported ToppleCat report presentation language.');
  const expectedMessageKeys = Object.keys(catalogs.en).sort();
  if (Object.keys(messages).sort().join('\u0000') !== expectedMessageKeys.join('\u0000')) {
    throw new Error('ToppleCat report localization catalog is incomplete.');
  }
  const t = (key, ...values) => {
    const template = messages[key];
    if (typeof template !== 'string') throw new Error(`Missing ToppleCat report message: ${key}`);
    return template.replace(/\{(\d+)\}/g, (_whole, index) => String(values[Number(index)] ?? ''));
  };
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
  const phaseName = phase => t(`phase.${phase in { GIVEN: true, WHEN: true, THEN: true, AND: true } ? phase : 'AND'}`);
  const parsedStep = step => {
    if (typeof step !== 'string') return step || {};
    const match = step.match(/^(Given|When|Then|And)\s+(.*)$/i);
    return match ? { phase: match[1].toUpperCase(), sentence: match[2] } : { phase: 'AND', sentence: step };
  };
  const scenario = (steps, live = false) => {
    const rows = (steps || []).map(parsedStep);
    if (!rows.length) return `<p class="meta">${t('scenario.unavailable')}</p>`;
    let previous = '';
    return `<div class="scenario" aria-label="${t('scenario.aria')}">${rows.map(step => {
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
        const safe = safeHref(destination); const link = safe ? ` <a href="${e(safe)}" target="_blank" rel="noopener">${t('image.openRemote')}</a>` : '';
        return `<div class="image-placeholder"${anchor}><strong>${t('image.unavailable')}</strong> ${inline(block.text || t('image.noAlt'))} ${e(block.title || '')}${link}</div>`;
      }
      case 'MERMAID': return `<section class="mermaid-panel"${anchor}><div class="mermaid-diagram"><div class="mermaid-source" hidden>${e(block.text)}</div></div><details><summary>${t('mermaid.source')}</summary>${code('mermaid', block.text)}</details></section>`;
      case 'CODE_FENCE': return `<section${anchor}>${code(block.language, block.text)}</section>`;
      default: return `<section${anchor}><p class="meta">${t('markdown.unrenderable')}</p>${code(block.language || 'markdown', block.text)}</section>`;
    }
  };
  const documentView = document => `<article class="document" id="document-${id(document.path)}"><p class="document-identity"><code>${e(document.path)}</code></p>${(document.blocks || []).map(markdownBlock).join('')}</article>`;
  const visibility = value => value === 'HIDDEN' ? `<span class="badge HIDDEN">${t('visibility.hidden')}</span>` : `<span class="badge PUBLIC">${t('visibility.public')}</span>`;
  const method = item => item?.sourceCode ? `<details><summary>${t('method.source')}</summary><p class="meta">${t('method.description')}</p>${item.methodIdentity ? `<p class="technical-meta"><code>${e(item.methodIdentity)}</code>${item.sourceFile ? `, ${e(item.sourceFile)}:${e(item.sourceLine)}` : ''}</p>` : ''}${code('java', item.sourceCode)}</details>` : `<p class="meta">${t('method.unavailable')}</p>`;
  const reviewCase = item => `<article class="case-card"><p>${visibility(item.visibility)} <strong>${e(item.caseId)}</strong></p>${scenario(item.scenario?.length ? item.scenario : [], false)}<div class="case-grid"><section><h4>${t('inputs')}</h4>${values(item.inputs)}</section><section><h4>${t('expectedResult')}</h4>${values(item.expected)}</section></div></article>`;
  const reviewProperties = properties => !(properties || []).length ? '' : `<section><h4>${t('property.declarations')}</h4>${properties.map(property => `<article class="case-card"><strong>${e(property.title)}</strong><p class="meta"><code>${e(property.methodIdentity)}</code>. ${t('property.tries', e(property.tries), e(property.maxDiscards), e(property.maxShrinks))}</p><details><summary>${t('property.details')}</summary><p class="technical-meta">${e(property.sourceFile)}:${e(property.sourceLine)}</p>${code('java', property.sourceCode)}</details></article>`).join('')}</section>`;
  const advisories = () => !(data.contractQualityAdvisories || []).length ? '' : `<section class="report-section" id="contract-quality-advisories"><h2>${t('advisory.heading')}</h2><p>${t('advisory.description')}</p>${data.contractQualityAdvisories.map(advisory => `<div class="advisory"><p><strong>${e(advisory.ruleCode)}</strong> ${t('advisory.for')} <a href="#ac-${id(advisory.acId)}">${e(advisory.acId)}</a></p><p>${e(advisory.expectedPath)}. ${t('advisory.publicRows', e(advisory.publicCount))} ${t('advisory.reviewerRows', e(advisory.hiddenCount))}</p></div>`).join('')}</section>`;
  const reviewAc = item => `<article class="ac-review" id="review-${id(item.acId)}"><div class="ac-heading"><span class="ac-id">${e(item.acId)}</span><h3>${e(item.title)}</h3></div>${item.location?.documentPath ? `<p class="meta">${t('review.selectedSdd')}: <code>${e(item.location.documentPath)}</code>, ${t('review.documentPosition', e(item.location.documentPosition))}</p>` : `<p class="meta">${t('review.noSpec')}</p>`}<h4>${t('review.material')}</h4>${(item.cases || []).map(reviewCase).join('') || `<p class="meta">${t('review.noRows')}</p>`}${reviewProperties(item.properties)}${method(item.method)}<details><summary>${t('review.techSummary')}</summary><p class="technical-meta">${t('review.techMeta')}</p></details></article>`;
  const reviewPage = () => {
    document.title = t('review.title'); document.getElementById('title').textContent = t('review.title'); document.getElementById('notice').textContent = t('review.notice');
    const docs = data.selectedSpecDocuments || [];
    const docIntro = docs.length ? t('review.withDocs') : t('review.withoutDocs');
    document.getElementById('summary').innerHTML = `<section class="report-intro"><h2>${t('review.notice')}</h2><p>${docIntro}</p></section>`;
    document.getElementById('report').innerHTML = `${docs.length ? `<section class="report-section" id="selected-documents"><h2>${t('review.selectedDocuments')}</h2>${docs.map(documentView).join('')}</section>` : ''}<section class="report-section" id="executable-material"><h2>${t('review.material')}</h2>${(data.acceptanceConditions || []).map(reviewAc).join('')}</section>${advisories()}`;
    document.getElementById('outline').innerHTML = `<h2>${t('review.outline')}</h2>${docs.map(doc => `<a href="#document-${id(doc.path)}">${e(doc.path)}</a>`).join('')}<a href="#executable-material">${t('review.material')}</a>${(data.acceptanceConditions || []).map(ac => `<a href="#review-${id(ac.acId)}">${e(ac.acId)}</a>`).join('')}${(data.contractQualityAdvisories || []).length ? `<a href="#contract-quality-advisories">${t('review.advisories')}</a>` : ''}`;
  };
  const gate = name => (data.gates || []).find(item => item.name === name) || { name, verdict: 'INCOMPLETE', reason: t('gate.evidenceUnavailable') };
  const gateCard = item => `<div class="gate-card ${e(item.verdict)}"><p>${badge(item.verdict)} <strong>${e(item.name)}</strong></p>${item.reason ? `<p>${t('gate.canonicalReason', `<code>${e(item.reason)}</code>`)}</p>` : ''}</div>`;
  const failedSteps = item => (item.steps || []).filter(step => step.status === 'FAIL');
  const comparison = item => {
    const step = failedSteps(item).find(candidate => (candidate.comparisons || []).length) || (item.steps || []).find(candidate => (candidate.comparisons || []).length);
    const comparisons = step?.comparisons || [];
    if (!comparisons.length) return '';
    return `<section class="comparison"><h4>${t('comparison.heading')}</h4><p class="meta">${t('comparison.boundTo')} <code>${e(step.stepId)}</code>.</p>${comparisons.map(entry => `<h5>${t('comparison.expected')} <code>${e(entry.expectedKey)}</code></h5><div class="table-wrap"><table><thead><tr><th>${t('comparison.path')}</th><th>${t('comparison.difference')}</th><th>${t('comparison.expected')}</th><th>${t('comparison.actual')}</th></tr></thead><tbody>${(entry.differences || []).map(diff => `<tr><td>${e(diff.path)}</td><td>${e(diff.kind)}</td><td>${e(JSON.stringify(diff.expected))}</td><td>${e(JSON.stringify(diff.actual))}</td></tr>`).join('')}</tbody></table></div>`).join('')}</section>`;
  };
  const stepData = item => {
    const recorded = (item.steps || []).filter(step => (step.actualArguments || []).length);
    if (!recorded.length) return '';
    return `<section class="step-data"><h4>${t('stepData.heading')}</h4>${recorded.map(step => `<details><summary>${t('stepData.arguments')} <code>${e(step.stepId)}</code></summary>${values(step.actualArguments)}</details>`).join('')}</section>`;
  };
  const lazyCases = new Map();
  const verificationCaseContent = (ac, item) => `<h4>${t('case.publicRule')}</h4><p><a href="#verification-${id(ac.acId)}">${e(ac.acId)}: ${e(ac.title)}</a></p><h4>${t('case.scenario')}</h4>${scenario(item.steps?.length ? item.steps.map(step => ({ ...step, phase: ac.stepPhases?.[step.stepId] || 'AND' })) : ac.scenario, Boolean(item.steps?.length))}${stepData(item)}<h4>${t('case.failedStep')}</h4>${(() => { const last = failedSteps(item)[0] || (item.steps || []).filter(step => step.status !== 'SKIPPED').at(-1); return last ? `<p><code>${e(last.stepId)}</code> ${e(last.sentence)}</p>` : `<p class="meta">${t('case.noStep')}</p>`; })()}${comparison(item)}<div class="case-grid"><section><h4>${t('inputs')}</h4>${values(item.inputs)}</section><section><h4>${t('case.completeExpected')}</h4>${values(item.expected)}</section></div><details class="raw-failure"><summary>${t('case.rawFailure')}</summary>${item.failure ? `<pre>${e(item.failure)}</pre>` : `<p class="meta">${t('case.noRawFailure')}</p>`}<h5>${t('case.expectedConsumption')}</h5>${values(item.expectedConsumption || {})}</details>`;
  const verificationCase = (ac, item, open) => {
    const key = JSON.stringify([ac.acId, item.caseId]);
    lazyCases.set(key, { ac, item });
    return `<details class="case-card" data-case-id="${e(item.caseId)}" data-case-status="${e(item.status)}" data-search="${e(`${ac.acId} ${ac.title} ${item.caseId}`.toLowerCase())}" data-lazy-case="${e(key)}"${open ? ' open' : ''} id="case-${id(item.caseId)}"><summary>${visibility(item.visibility)} <strong>${e(item.caseId)}</strong> ${badge(item.status)}</summary><div class="lazy-case-content">${open ? verificationCaseContent(ac, item) : ''}</div></details>`;
  };
  const acCases = (visibilityName, openFailure) => (data.acceptanceConditions || []).flatMap(ac => (ac.cases || []).filter(item => item.visibility === visibilityName).map(item => ({ ac, item }))).map(({ ac, item }, index) => verificationCase(ac, item, openFailure && index === 0 && item.status === 'FAIL')).join('') || `<p class="meta">${t('case.noCurrentRows')}</p>`;
  const integrityFailed = () => gate('CONTRACT_INTEGRITY').verdict !== 'PASS';
  const propertyResults = () => (data.acceptanceConditions || []).flatMap(ac => (ac.properties || []).map(property => `<article class="case-card" id="property-${id(property.methodIdentity)}"><h3>${e(property.title)} ${badge(property.status)}</h3><p class="meta"><code>${e(property.methodIdentity)}</code>. ${t('property.results', e(property.completedTrials) + '/' + e(property.requestedTrials), e(property.discards))}</p>${property.incompleteReason ? `<p>${e(property.incompleteReason)}</p>` : ''}${property.originalCounterexample ? `<h4>${t('property.original')}</h4>${code('json', property.originalCounterexample.choicesJson)}` : ''}${property.shrunkCounterexample ? `<h4>${t('property.shrunk')}</h4>${code('json', property.shrunkCounterexample.choicesJson)}` : ''}${property.replayToken ? `<p class="meta">${t('property.replay')} <code>${e(property.replayToken)}</code></p>` : ''}</article>`)).join('') || `<p class="meta">${t('property.none')}</p>`;
  const safeguardStatus = (ac, visibilityName) => {
    const rows = (ac.cases || []).filter(item => item.visibility === visibilityName);
    return rows.some(item => item.status === 'FAIL') ? 'FAIL' : rows.length && rows.every(item => item.status === 'PASS') ? 'PASS' : 'NOT_REPORTED';
  };
  const safeguardResult = (ac, visibilityName, heading, gateName) => {
    const status = safeguardStatus(ac, visibilityName);
    const gateValue = gate(gateName);
    const label = status === 'PASS' ? t('acResults.passed') : status === 'FAIL' ? t('acResults.failed') : t('acResults.noData');
    const reason = status === 'NOT_REPORTED' && ['DISABLED', 'NOT_APPLICABLE', 'INCOMPLETE'].includes(gateValue.verdict) && gateValue.reason
      ? `<p class="meta">${t('acResults.recordedReason', e(gateValue.reason))}</p>` : '';
    return `<section class="safeguard-result"><h4>${heading}</h4><p>${label}</p>${reason}</section>`;
  };
  const mutationNoData = gateValue => {
    const reason = !data.mutationAttribution && ['DISABLED', 'NOT_APPLICABLE', 'INCOMPLETE'].includes(gateValue.verdict) && gateValue.reason
      ? `<p class="meta">${t('acResults.recordedReason', e(gateValue.reason))}</p>` : '';
    return `<p>${t('mutation.noData')}</p>${reason}`;
  };
  const mutationResult = (assessment, gateValue) => {
    if (!assessment || assessment.attributionGap) return mutationNoData(gateValue);
    const message = assessment.detectionRate >= assessment.sealedThreshold ? 'mutation.meets' : 'mutation.below';
    return `<p><strong>${t(assessment.detectionRate >= assessment.sealedThreshold ? 'mutation.meetsResult' : 'mutation.belowResult')}</strong></p><p>${t(message, e(assessment.coveredMutantCount), e(assessment.killedByAcceptanceMethodMutantCount), e(assessment.sealedThreshold))}</p>`;
  };
  const mutationTechnicalDetails = (value, gateValue) => {
    if (!value) return `<details id="mutation-technical-details"><summary>${t('mutation.technicalDetails')}</summary>${gateCard(gateValue)}</details>`;
    const outcome = rows => !rows?.length ? '' : `<div class="table-wrap"><table><thead><tr><th>${t('mutation.pitStatus')}</th><th>${t('mutation.detected')}</th><th>${t('mutation.mutants')}</th></tr></thead><tbody>${rows.map(row => `<tr><td><code>${e(row.status)}</code></td><td>${e(row.detected)}</td><td>${e(row.count)}</td></tr>`).join('')}</tbody></table></div>`;
    const detected = (value.mutations || []).filter(item => item.detected).length;
    return `<details id="mutation-technical-details"><summary>${t('mutation.technicalDetails')}</summary>${gateCard(gateValue)}<p>${t('mutation.profile', `<code>${e(value.pitVersion)}</code>`, `<code>${e(value.managedProfileId)}</code>`)}</p><h3>${t('mutation.global')}</h3><p>${t('mutation.detectedCount', e(detected), e(value.producerMutationCount))}</p><p class="meta">${t('mutation.globalMeaning')}</p><p>${t('mutation.producerCounts', e(value.producerMutationCount), e(value.uniquelyAttributedMutationCount), e(value.unattributedMutationCount))}</p><h3>${t('mutation.operatorIds')}</h3><ul>${(value.managedOperatorIds || []).map(operator => `<li><code>${e(operator)}</code></li>`).join('')}</ul><h3>${t('mutation.rawOutcomes')}</h3>${outcome(value.producerOutcomeCounts)}<h3>${t('mutation.perAc')}</h3><p class="meta">${t('mutation.perAcMeaning')}</p><div class="table-wrap"><table><thead><tr><th>${t('mutation.ac')}</th><th>${t('mutation.covered')}</th><th>${t('mutation.detectedByMethod')}</th><th>${t('mutation.threshold')}</th><th>${t('mutation.rate')}</th></tr></thead><tbody>${(value.assessments || []).map(assessment => `<tr><td>${e(assessment.acId)}</td><td>${e(assessment.coveredMutantCount)}</td><td>${e(assessment.killedByAcceptanceMethodMutantCount)}</td><td>${e(assessment.sealedThreshold)}%</td><td>${e(assessment.detectionRate)}%</td></tr>`).join('')}</tbody></table></div><h3>${t('mutation.rawFindings')}</h3>${(value.mutations || []).map((item, index) => `<article class="case-card"><h4>${t('mutation.mutant', index + 1)}: <code>${e(item.status)}</code></h4><p>${e(item.description)}</p><p class="meta">${t('mutation.mutator', `<code>${e(item.mutator)}</code>`, e(item.detected))}</p><h5>${t('mutation.coveringTests')}</h5><ul>${(item.coveringTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul><h5>${t('mutation.killingTests')}</h5><ul>${(item.killingTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul><h5>${t('mutation.succeedingTests')}</h5><ul>${(item.succeedingTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul></article>`).join('')}</details>`;
  };
  const mutation = () => {
    const value = data.mutationAttribution;
    const gateValue = gate('MUTATION');
    const assessments = new Map((value?.assessments || []).map(assessment => [assessment.acId, assessment]));
    const acs = [...(data.acceptanceConditions || [])];
    const known = new Set(acs.map(ac => ac.acId));
    assessments.forEach((assessment, acId) => { if (!known.has(acId)) acs.push({ acId, title: '', cases: [] }); });
    acs.sort((left, right) => String(left.acId).localeCompare(String(right.acId)));
    const results = acs.map(ac => `<article class="ac-review mutation-ac-result" id="mutation-ac-${id(ac.acId)}"><div class="ac-heading"><span class="ac-id">${e(ac.acId)}</span>${ac.title ? `<h3>${e(ac.title)}</h3>` : ''}</div><div class="safeguard-results">${safeguardResult(ac, 'PUBLIC', t('acResults.publicAcceptance'), 'JUNIT')}${safeguardResult(ac, 'HIDDEN', t('acResults.hiddenTests'), 'REVIEWER_JUNIT')}<section class="safeguard-result mutation-result"><h4>${t('acResults.mutationTesting')}</h4>${mutationResult(assessments.get(ac.acId), gateValue)}</section></div></article>`).join('') || `<p class="meta">${t('mutation.noData')}</p>`;
    return `<p>${t('mutation.intro')}</p>${results}${mutationTechnicalDetails(value, gateValue)}`;
  };
  const problems = () => {
    if (data.verdict === 'PASS') return '';
    const gates = integrityFailed()
      ? [gate('CONTRACT_INTEGRITY')]
      : (data.gates || []).filter(item => item.verdict === 'FAIL').concat((data.gates || []).filter(item => item.verdict === 'INCOMPLETE'));
    const caseProblems = integrityFailed() ? [] : (data.acceptanceConditions || []).flatMap(ac => (ac.cases || []).filter(item => item.status === 'FAIL').map(item => ({ ac, item })));
    const all = gates.map(item => `<li><a href="#${item.name === 'CONTRACT_INTEGRITY' ? 'contract-integrity' : item.name === 'JUNIT' || item.name === 'EXPECTED_CONSUMPTION' ? 'public-acceptance' : item.name === 'REVIEWER_JUNIT' ? 'hidden-tests' : item.name === 'PROPERTY' ? 'property-testing' : 'mutation-testing'}">${e(item.name)}</a>: ${item.reason ? t('gate.canonicalReason', `<code>${e(item.reason)}</code>`) : t('problems.defaultGate', e(item.verdict))}</li>`).concat(caseProblems.map(({ ac, item }) => `<li><a href="#case-${id(item.caseId)}">${e(ac.acId)} / ${e(item.caseId)}</a>: ${t(item.visibility === 'HIDDEN' ? 'problems.hiddenCase' : 'problems.publicCase')}</li>`));
    return `<section class="problem-summary" id="problems"><h2>${t('problems.heading')}</h2><ol>${all.join('') || `<li>${t('problems.noEvidence')}</li>`}</ol></section>`;
  };
  const verificationPage = () => {
    document.title = t('verification.title'); document.getElementById('title').textContent = t('verification.title');
    const conclusion = data.verdict === 'PASS' ? t('verification.pass') : data.verdict === 'FAIL' ? t('verification.fail') : t('verification.incomplete');
    document.getElementById('notice').textContent = conclusion;
    const run = data.run || {}; const scope = data.deliveryScope || {}; const selected = (scope.acceptanceConditionIds || []).join(', ') || t('verification.fullContract');
    document.getElementById('summary').innerHTML = `<section class="report-intro verification ${e(data.verdict)}"><h2>${e(conclusion)}</h2><p>${t('verification.aggregate', badge(data.verdict), e(run.failedGateCount ?? 0), e(run.incompleteGateCount ?? 0), e(run.failedAcceptanceConditionCount ?? 0), e(run.failedCaseCount ?? 0))}</p><p class="meta">${t('verification.run', `<code>${e(run.runId || t('verification.unavailable'))}</code>`, e(run.startedAt || t('verification.unavailable')), e(run.finishedAt || data.generatedAt || t('verification.unavailable')))}</p><p class="meta">${t('verification.scope', e(selected), e(scope.executedHiddenRows ?? 0), e(scope.executedPublicProperties ?? 0))}</p></section><section class="filter-controls" aria-label="${t('verification.filters')}"><label>${t('verification.find')} <input id="case-query" type="search" autocomplete="off"></label>${['FAIL','PASS','NOT_REPORTED'].map(status => `<button type="button" data-status-filter="${status}" aria-pressed="false">${status}</button>`).join('')}</section>${problems()}`;
    const blocked = `<p class="suppressed">${t('verification.blocked')}</p>`;
    document.getElementById('report').innerHTML = `<section class="report-section" id="contract-integrity"><h2>${t('verification.contractIntegrity')}</h2>${gateCard(gate('CONTRACT_INTEGRITY'))}</section><section class="report-section" id="public-acceptance"><h2>${t('verification.publicAcceptance')}</h2>${integrityFailed() ? blocked : `${gateCard(gate('JUNIT'))}${gateCard(gate('EXPECTED_CONSUMPTION'))}${acCases('PUBLIC', true)}`}</section><section class="report-section" id="hidden-tests"><h2>${t('verification.hiddenTests')}</h2>${integrityFailed() ? blocked : `${gateCard(gate('REVIEWER_JUNIT'))}${acCases('HIDDEN', true)}`}</section><section class="report-section" id="property-testing"><h2>${t('verification.propertyTesting')}</h2>${integrityFailed() ? blocked : `${gateCard(gate('PROPERTY'))}${propertyResults()}`}</section><section class="report-section" id="mutation-testing"><h2>${t('verification.mutationTesting')}</h2>${integrityFailed() ? blocked : mutation()}</section>`;
    document.getElementById('outline').innerHTML = `<h2>${t('verification.outline')}</h2><a href="#problems">${t('problems.heading')}</a><a href="#contract-integrity">${t('verification.contractIntegrity')}</a><a href="#public-acceptance">${t('verification.publicAcceptance')}</a><a href="#hidden-tests">${t('verification.hiddenTests')}</a><a href="#property-testing">${t('verification.propertyTesting')}</a><a href="#mutation-testing">${t('verification.mutationTesting')}</a>`;
  };
  document.getElementById('skip-link').textContent = t('shell.skipLink');
  document.getElementById('outline').setAttribute('aria-label', t('shell.outline'));
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
    try { container.innerHTML = window.ToppleCatMermaid.render(source); } catch (_error) { container.innerHTML = `<p class="mermaid-error">${t('mermaid.error')}</p>`; }
  });
})();
