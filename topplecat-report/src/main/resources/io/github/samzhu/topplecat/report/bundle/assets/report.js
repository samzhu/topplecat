(() => {
  const data = JSON.parse(document.getElementById('topplecat-report-data').textContent);
  const presentation = JSON.parse(document.getElementById('topplecat-report-presentation').textContent);
  const catalogs = {
    en: {
      'property.discarded': 'Discarded generator inputs', 'property.discardedReason': "This input did not meet this run's generated-input range.", 'property.previous': 'Previous page', 'property.next': 'Next page', 'property.page': 'Page {0} of {1}',
      'mutation.detectedAll': 'Every attributed altered program made the public acceptance fail as expected.', 'mutation.survived': '{0} altered programs failed as expected, but {1} still passed. The public acceptance did not find those simulated errors, so this function may have a problem that the current acceptance does not reveal.', 'mutation.noAttributed': 'No mutation was exactly attributed to this AC in the current run.',
      'technical.heading': 'Technical evidence', 'technical.description': 'Canonical Gate names, run metadata, and producer details remain here for audit. They do not replace the AC explanations above.', 'verification.needsAttention': 'Needs attention', 'verification.allAcs': 'All ACs', 'verification.acResult': 'Verification result', 'verification.incompleteAc': 'Verification lacks evidence for this AC.', 'verification.failedAc': 'Verification failed for this AC.', 'verification.passedAc': 'Verification passed for this AC.',
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
      'property.none': 'No Property declaration applied to this scope.', 'property.results': '{0} generated inputs completed; {1} were discarded.', 'property.original': 'Original counterexample', 'property.shrunk': 'Simplified counterexample', 'property.replay': 'Replay token', 'property.discarded': 'Discarded generator inputs', 'property.discardedReason': "This input did not meet this run's generated-input range.", 'property.previous': 'Previous page', 'property.next': 'Next page', 'property.page': 'Page {0} of {1}',
      'acResults.publicAcceptance': 'Public Acceptance', 'acResults.hiddenTests': 'Hidden Tests', 'acResults.expectedResult': 'Expected Result Check', 'acResults.propertyTesting': 'Property-Based Testing', 'acResults.mutationTesting': 'Mutation Testing', 'acResults.passed': 'Passed', 'acResults.failed': 'Failed', 'acResults.incomplete': 'Lacks evidence', 'acResults.disabled': 'Disabled', 'acResults.notApplicable': 'Not applicable', 'acResults.recordedReason': 'The current run recorded: {0}', 'acResults.publicCases': 'Public Acceptance cases', 'acResults.hiddenCases': 'Hidden Test cases',
      'mutation.intro': "ToppleCat deliberately changed production logic and reran this AC's unchanged public acceptance.", 'mutation.detectedAll': 'Every attributed altered program made the public acceptance fail as expected.', 'mutation.survived': '{0} altered programs failed as expected, but {1} still passed. The public acceptance did not find those simulated errors, so this function may have a problem that the current acceptance does not reveal.', 'mutation.noAttributed': 'No mutation was exactly attributed to this AC in the current run.', 'mutation.technicalDetails': 'Mutation technical evidence', 'mutation.pitStatus': 'PIT status', 'mutation.detected': 'Detected', 'mutation.mutants': 'Mutants', 'mutation.profile': 'PIT {0}, managed profile {1}.', 'mutation.global': 'PIT producer outcome', 'mutation.detectedCount': '{0} producer mutations were detected by at least one test.', 'mutation.globalMeaning': 'This PIT-wide observation does not mean that every Acceptance Method detected every mutation.', 'mutation.producerCounts': '{0} producer mutations. {1} uniquely attributed. {2} unattributed.', 'mutation.operatorIds': 'Managed operator IDs', 'mutation.perAc': 'Per-AC attributed mutation assessment', 'mutation.perAcMeaning': 'Each selected AC is assessed only through its own public Acceptance Method. A different AC cannot supply detection credit.', 'mutation.ac': 'AC', 'mutation.covered': 'Attributed', 'mutation.detectedByMethod': 'Failed as expected', 'mutation.rawOutcomes': 'Raw producer outcomes', 'mutation.rawFindings': 'Raw PIT findings', 'mutation.mutant': 'Mutation {0}', 'mutation.mutator': 'Mutator {0}. Detected: {1}.', 'mutation.coveringTests': 'coveringTests', 'mutation.killingTests': 'killingTests', 'mutation.succeedingTests': 'succeedingTests',
      'technical.heading': 'Technical evidence', 'technical.description': 'Canonical Gate names, run metadata, and producer details remain here for audit. They do not replace the AC explanations above.', 'problems.heading': 'Needs attention', 'problems.noEvidence': 'Verification lacks evidence for the current run.', 'verification.title': 'Verification Report', 'verification.pass': 'Verification passed', 'verification.fail': 'Verification failed', 'verification.incomplete': 'Verification lacks evidence', 'verification.aggregate': '{0} selected ACs: {1} passed, {2} failed, {3} lack evidence.', 'verification.needsAttention': 'Needs attention', 'verification.allAcs': 'All ACs', 'verification.acResult': 'Verification result', 'verification.incompleteAc': 'Verification lacks evidence for this AC.', 'verification.failedAc': 'Verification failed for this AC.', 'verification.passedAc': 'Verification passed for this AC.', 'verification.unavailable': 'unavailable', 'verification.run': 'Run ID: {0}. Started: {1}. Finished: {2}.', 'verification.fullContract': 'Full executable contract', 'verification.scope': 'Selected and executed scope: {0}. Hidden rows: {1}. Properties: {2}.', 'verification.filters': 'Verification report filters', 'verification.find': 'Find AC or case', 'verification.contractMatchesSeal': 'Contract Integrity passed: the selected executable contract matches its Mechanical Seal.', 'verification.contractMismatch': 'Contract Integrity failed: the selected executable contract no longer matches its Mechanical Seal, so downstream AC work did not run.', 'verification.contractUnverified': 'Contract Integrity lacks trustworthy current-run evidence, so downstream AC work did not run.', 'verification.contractIntegrity': 'Contract Integrity', 'verification.publicAcceptance': 'Public Acceptance', 'verification.hiddenTests': 'Hidden Tests', 'verification.propertyTesting': 'Property-Based Testing', 'verification.mutationTesting': 'Mutation Testing', 'verification.outline': 'Verification',
      'shell.skipLink': 'Skip to report content', 'shell.outline': 'Report outline'
    },
    'zh-TW': {
      'property.discarded': '被捨棄的產生器輸入', 'property.discardedReason': '這個輸入不符合本次執行的產生輸入範圍。', 'property.previous': '上一頁', 'property.next': '下一頁', 'property.page': '第 {0} 頁，共 {1} 頁',
      'mutation.detectedAll': '每個歸因到的改動程式都讓公開驗收如預期失敗。', 'mutation.survived': '{0} 個改動程式如預期失敗，但仍有 {1} 個通過。公開驗收沒有找到那些模擬錯誤，因此這個函式可能存在目前驗收無法揭露的問題。', 'mutation.noAttributed': '本次執行沒有突變被精確歸因到這個 AC。',
      'technical.heading': '技術證據', 'technical.description': 'Canonical Gate 名稱、執行中繼資料與 producer 細節保留在此供稽核；它們不取代上方的 AC 說明。', 'verification.needsAttention': '需要注意', 'verification.allAcs': '所有 AC', 'verification.acResult': '驗證結果', 'verification.incompleteAc': '這個 AC 的驗證缺少證據。', 'verification.failedAc': '這個 AC 的驗證失敗。', 'verification.passedAc': '這個 AC 的驗證通過。',
      'verification.pass': '驗證通過', 'verification.fail': '驗證失敗', 'verification.incomplete': '驗證缺少證據', 'verification.aggregate': '{0} 個選定 AC：{1} 個通過、{2} 個失敗、{3} 個缺少證據。',
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
      'property.none': '此範圍沒有適用的 Property 宣告。', 'property.results': '{0} 個產生輸入完成；其中 {1} 個被捨棄。', 'property.original': '原始反例', 'property.shrunk': '簡化後反例', 'property.replay': '重播權杖', 'property.discarded': '被捨棄的產生器輸入', 'property.discardedReason': '這個輸入不符合本次執行的產生輸入範圍。', 'property.previous': '上一頁', 'property.next': '下一頁', 'property.page': '第 {0} 頁，共 {1} 頁',
      'acResults.publicAcceptance': '公開驗收', 'acResults.hiddenTests': '隱藏測試', 'acResults.expectedResult': '預期結果檢查', 'acResults.propertyTesting': '性質導向測試', 'acResults.mutationTesting': '突變測試', 'acResults.passed': '通過', 'acResults.failed': '失敗', 'acResults.incomplete': '缺少證據', 'acResults.disabled': '已停用', 'acResults.notApplicable': '不適用', 'acResults.recordedReason': '本次執行記錄：{0}', 'acResults.publicCases': '公開驗收案例', 'acResults.hiddenCases': '隱藏測試案例',
      'mutation.intro': '驗證時，ToppleCat 暫時改變正式程式的行為，再用這個 AC 未改變的公開驗收檢查。', 'mutation.technicalDetails': '突變測試技術證據', 'mutation.pitStatus': 'PIT 狀態', 'mutation.detected': '已偵測', 'mutation.mutants': '突變體', 'mutation.profile': 'PIT {0}，受管理設定檔 {1}。', 'mutation.global': 'PIT producer 結果', 'mutation.detectedCount': '{0} 個 producer 突變體至少被一項測試偵測到。', 'mutation.globalMeaning': '這個 PIT 全域觀察不代表每個驗收方法都偵測到每個突變體。', 'mutation.producerCounts': '{0} 個 producer 突變體；{1} 個獲得唯一歸因；{2} 個未歸因。', 'mutation.operatorIds': '受管理運算子 ID', 'mutation.rawOutcomes': '原始 producer 結果', 'mutation.perAc': '每個 AC 的歸因突變評估', 'mutation.perAcMeaning': '每個選定 AC 只透過自己的公開驗收方法評估；其他 AC 不能提供偵測信用。', 'mutation.ac': 'AC', 'mutation.covered': '已歸因', 'mutation.detectedByMethod': '如預期失敗', 'mutation.rawFindings': '原始 PIT 發現', 'mutation.mutant': '突變體 {0}', 'mutation.mutator': 'Mutator {0}。已偵測：{1}。', 'mutation.coveringTests': 'coveringTests', 'mutation.killingTests': 'killingTests', 'mutation.succeedingTests': 'succeedingTests',
      'problems.defaultGate': '這個 Gate 在這次執行記錄為 {0}。', 'problems.hiddenCase': '這個審閱者型別資料列在執行所記錄的情境時失敗。', 'problems.publicCase': '這個公開型別資料列在執行所記錄的情境時失敗。', 'problems.heading': '問題摘要', 'problems.noEvidence': '驗證不完整，因為本次執行證據無法使用。',
      'verification.title': '驗證報告', 'verification.pass': '驗證通過', 'verification.fail': '驗證失敗', 'verification.incomplete': '驗證缺少證據', 'verification.aggregate': '{0} 個選定 AC：{1} 個通過、{2} 個失敗、{3} 個缺少證據。', 'verification.unavailable': '無法使用', 'verification.run': '執行 ID：{0}。開始：{1}。完成：{2}。', 'verification.fullContract': '完整可執行契約', 'verification.scope': '已選並執行的範圍：{0}。隱藏資料列：{1}。Property：{2}。', 'verification.filters': '驗證報告篩選器', 'verification.find': '尋找 AC 或案例', 'verification.contractMatchesSeal': '契約完整性已通過：已選可執行契約符合其機械封印。', 'verification.contractMismatch': '契約完整性失敗：已選可執行契約已不符合其機械封印，因此未執行下游 AC 工作。', 'verification.contractUnverified': '契約完整性缺少可信的本次執行證據，因此未執行下游 AC 工作。', 'verification.contractIntegrity': '契約完整性', 'verification.publicAcceptance': '公開驗收', 'verification.hiddenTests': '隱藏測試', 'verification.propertyTesting': '性質導向測試', 'verification.mutationTesting': '突變測試', 'verification.outline': '驗證',
      'shell.skipLink': '跳至報告內容', 'shell.outline': '報告大綱'
    }
  };
  const mutationDetailMessages = {
    en: {
      'mutation.summary': 'This AC was assessed against {0} attributed changes: {1} detected, {2} undetected.',
      'mutation.cardTitle': 'Undetected mutation {0}',
      'mutation.whatChanged': 'What changed?',
      'mutation.exactReplacement': 'The operator changed from {0} to {1}.',
      'mutation.reportedDescription': 'Reported mutation description: {0}.',
      'mutation.noExactReplacement': 'ToppleCat cannot safely state an exact before/after replacement from the available source context.',
      'mutation.whereChanged': 'Where?',
      'mutation.productionClass': 'Production class: {0}',
      'mutation.sourceFile': 'Source file: {0}',
      'mutation.method': 'Method: {0}',
      'mutation.descriptor': 'Descriptor: {0}',
      'mutation.line': 'Line: {0}',
      'mutation.originalLine': 'Original source line',
      'mutation.rawCoordinates': 'Mutation coordinates and selector relations',
      'mutation.block': 'Bytecode block: {0}',
      'mutation.bytecodeIndex': 'Bytecode index: {0}',
      'mutation.attributedAc': 'Attributed Acceptance Conditions',
      'mutation.detectedAc': 'Detected by Acceptance Conditions',
      'mutation.noLocation': 'PIT did not provide a unique source location for this mutation.',
      'mutation.whatHappened': 'What happened?',
      'mutation.producerOutcome': 'PIT reported the producer outcome as {0}.',
      'mutation.acPassed': "This AC's unchanged public acceptance still passed."
    },
    'zh-TW': {
      'mutation.summary': '這個 AC 共評估 {0} 個已歸因改動：偵測到 {1} 個，未偵測到 {2} 個。',
      'mutation.cardTitle': '未偵測到的突變 {0}',
      'mutation.whatChanged': '改變了什麼？',
      'mutation.exactReplacement': '運算子從 {0} 改成 {1}。',
      'mutation.reportedDescription': '回報的突變描述：{0}。',
      'mutation.noExactReplacement': '根據目前可取得的原始碼脈絡，ToppleCat 無法安全地說明確切的前後替換。',
      'mutation.whereChanged': '在哪裡？',
      'mutation.productionClass': '正式類別：{0}',
      'mutation.sourceFile': '原始碼檔案：{0}',
      'mutation.method': '方法：{0}',
      'mutation.descriptor': '描述：{0}',
      'mutation.line': '行號：{0}',
      'mutation.originalLine': '原始碼行',
      'mutation.rawCoordinates': '突變位置與選擇器關係',
      'mutation.block': '位元組碼區塊：{0}',
      'mutation.bytecodeIndex': '位元組碼索引：{0}',
      'mutation.attributedAc': '歸因到的驗收條件',
      'mutation.detectedAc': '被哪些驗收條件偵測到',
      'mutation.noLocation': 'PIT 沒有提供這個突變的唯一原始碼位置。',
      'mutation.whatHappened': '發生了什麼？',
      'mutation.producerOutcome': 'PIT 回報 producer 結果為 {0}。',
      'mutation.acPassed': '這個 AC 未改變的公開驗收仍然通過。'
    }
  };
  const language = String(presentation.language || '');
  const messages = catalogs[language];
  if (!messages) throw new Error('Unsupported ToppleCat report presentation language.');
  const expectedMessageKeys = Object.keys(catalogs.en).sort();
  if (!expectedMessageKeys.every(key => Object.prototype.hasOwnProperty.call(messages, key))) {
    throw new Error('ToppleCat report localization catalog is incomplete.');
  }
  const t = (key, ...values) => {
    const template = messages[key] ?? mutationDetailMessages[language]?.[key];
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
  const contractIntegritySummary = () => {
    const verdict = gate('CONTRACT_INTEGRITY').verdict;
    return verdict === 'PASS'
      ? t('verification.contractMatchesSeal')
      : verdict === 'FAIL'
        ? t('verification.contractMismatch')
        : t('verification.contractUnverified');
  };
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
    return `<p>${t('mutation.noAttributed')}</p>${reason}`;
  };
  const mutationResult = (assessment, gateValue) => {
    if (!assessment || assessment.attributionGap) return mutationNoData(gateValue);
    return assessment.killedByAcceptanceMethodMutantCount < assessment.coveredMutantCount
      ? `<p>${t('mutation.survived', e(assessment.killedByAcceptanceMethodMutantCount), e(assessment.coveredMutantCount - assessment.killedByAcceptanceMethodMutantCount))}</p>`
      : `<p>${t('mutation.detectedAll')}</p>`;
  };
  const mutationTechnicalDetails = (value, gateValue) => {
    if (!value) return `<details id="mutation-technical-details"><summary>${t('mutation.technicalDetails')}</summary>${gateCard(gateValue)}</details>`;
    const outcome = rows => !rows?.length ? '' : `<div class="table-wrap"><table><thead><tr><th>${t('mutation.pitStatus')}</th><th>${t('mutation.detected')}</th><th>${t('mutation.mutants')}</th></tr></thead><tbody>${rows.map(row => `<tr><td><code>${e(row.status)}</code></td><td>${e(row.detected)}</td><td>${e(row.count)}</td></tr>`).join('')}</tbody></table></div>`;
    const detected = (value.mutations || []).filter(item => item.detected).length;
    return `<details id="mutation-technical-details"><summary>${t('mutation.technicalDetails')}</summary>${gateCard(gateValue)}<p>${t('mutation.profile', `<code>${e(value.pitVersion)}</code>`, `<code>${e(value.managedProfileId)}</code>`)}</p><h3>${t('mutation.global')}</h3><p>${t('mutation.detectedCount', e(detected), e(value.producerMutationCount))}</p><p class="meta">${t('mutation.globalMeaning')}</p><p>${t('mutation.producerCounts', e(value.producerMutationCount), e(value.uniquelyAttributedMutationCount), e(value.unattributedMutationCount))}</p><h3>${t('mutation.operatorIds')}</h3><ul>${(value.managedOperatorIds || []).map(operator => `<li><code>${e(operator)}</code></li>`).join('')}</ul><h3>${t('mutation.rawOutcomes')}</h3>${outcome(value.producerOutcomeCounts)}<h3>${t('mutation.perAc')}</h3><p class="meta">${t('mutation.perAcMeaning')}</p><div class="table-wrap"><table><thead><tr><th>${t('mutation.ac')}</th><th>${t('mutation.covered')}</th><th>${t('mutation.detectedByMethod')}</th></tr></thead><tbody>${(value.assessments || []).map(assessment => `<tr><td>${e(assessment.acId)}</td><td>${e(assessment.coveredMutantCount)}</td><td>${e(assessment.killedByAcceptanceMethodMutantCount)}</td></tr>`).join('')}</tbody></table></div><h3>${t('mutation.rawFindings')}</h3>${(value.mutations || []).map((item, index) => `<article class="case-card"><h4>${t('mutation.mutant', index + 1)}: <code>${e(item.status)}</code></h4><p>${e(item.description)}</p><p class="meta">${t('mutation.mutator', `<code>${e(item.mutator)}</code>`, e(item.detected))}</p><h5>${t('mutation.coveringTests')}</h5><ul>${(item.coveringTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul><h5>${t('mutation.killingTests')}</h5><ul>${(item.killingTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul><h5>${t('mutation.succeedingTests')}</h5><ul>${(item.succeedingTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul></article>`).join('')}</details>`;
  };
  const mutation = () => {
    const value = data.mutationAttribution;
    const gateValue = gate('MUTATION');
    const assessments = new Map((value?.assessments || []).map(assessment => [assessment.acId, assessment]));
    const acs = [...(data.acceptanceConditions || [])];
    const known = new Set(acs.map(ac => ac.acId));
    assessments.forEach((assessment, acId) => { if (!known.has(acId)) acs.push({ acId, title: '', cases: [] }); });
    acs.sort((left, right) => String(left.acId).localeCompare(String(right.acId)));
    const results = acs.map(ac => `<article class="ac-review mutation-ac-result" id="mutation-ac-${id(ac.acId)}"><div class="ac-heading"><span class="ac-id">${e(ac.acId)}</span>${ac.title ? `<h3>${e(ac.title)}</h3>` : ''}</div><div class="safeguard-results">${safeguardResult(ac, 'PUBLIC', t('acResults.publicAcceptance'), 'JUNIT')}${safeguardResult(ac, 'HIDDEN', t('acResults.hiddenTests'), 'REVIEWER_JUNIT')}<section class="safeguard-result mutation-result"><h4>${t('acResults.mutationTesting')}</h4>${mutationResult(assessments.get(ac.acId), gateValue)}</section></div></article>`).join('') || `<p class="meta">${t('mutation.noAttributed')}</p>`;
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
  const safeguardFor = (ac, name) => (ac.safeguards || []).find(item => item.name === name) || { name, verdict: 'INCOMPLETE', explanation: t('gate.evidenceUnavailable'), technicalGate: '' };
  const safeguardLabel = safeguard => safeguard.verdict === 'PASS' ? t('acResults.passed') : safeguard.verdict === 'FAIL' ? t('acResults.failed') : safeguard.verdict === 'DISABLED' ? t('acResults.disabled') : safeguard.verdict === 'NOT_APPLICABLE' ? t('acResults.notApplicable') : t('acResults.incomplete');
  const safeguardCard = (ac, name, title, body = '') => {
    const safeguard = safeguardFor(ac, name);
    const detail = body || `<p>${e(safeguard.explanation || t('acResults.recordedReason', e(gate(safeguard.technicalGate).reason || '')))}</p>`;
    return `<section class="ac-safeguard ${e(safeguard.verdict)}" id="ac-${id(ac.acId)}-${id(name.toLowerCase())}"><h4>${e(title)} ${badge(safeguardLabel(safeguard))}</h4>${detail}</section>`;
  };
  const casesFor = (ac, visibilityName) => (ac.cases || []).filter(item => item.visibility === visibilityName);
  const casesMarkup = (ac, visibilityName) => {
    const rows = casesFor(ac, visibilityName);
    return rows.length ? rows.map(item => verificationCase(ac, item, item.status === 'FAIL')).join('') : `<p class="meta">${t('case.noCurrentRows')}</p>`;
  };
  const discardedInputs = property => {
    const inputs = property.discardedInputs || [];
    if (!inputs.length) return '';
    return `<details class="discarded-inputs"><summary>${t('property.discarded')} (${e(inputs.length)})</summary><p class="meta">${t('property.discardedReason')}</p><ol>${inputs.map(item => `<li data-discard-item>${code('json', item.choicesJson)}</li>`).join('')}</ol><div class="discarded-pages"><button type="button" data-discard-prev>${t('property.previous')}</button><span data-discard-page></span><button type="button" data-discard-next>${t('property.next')}</button></div></details>`;
  };
  const propertyResultsFor = ac => {
    const rows = (ac.properties || []).map(property => {
      const original = property.originalCounterexample
        ? `<h4>${t('property.original')}</h4>${code('json', property.originalCounterexample.choicesJson)}`
        : '';
      const shrunk = property.shrunkCounterexample
        ? `<h4>${t('property.shrunk')}</h4>${code('json', property.shrunkCounterexample.choicesJson)}`
        : '';
      const replay = property.replayToken
        ? `<p class="meta">${t('property.replay')} <code>${e(property.replayToken)}</code></p>`
        : '';
      const incomplete = property.incompleteReason ? `<p>${e(property.incompleteReason)}</p>` : '';
      return `<article class="case-card property-result" id="property-${id(property.methodIdentity)}"><h3>${e(property.title)} ${badge(property.status)}</h3><p class="meta">${t('property.results', e(`${property.completedTrials}/${property.requestedTrials}`), e(property.discards))}</p>${incomplete}${original}${shrunk}${replay}${discardedInputs(property)}</article>`;
    });
    return rows.join('') || `<p class="meta">${t('property.none')}</p>`;
  };
  const mutationFor = ac => (data.mutationAttribution?.assessments || []).find(item => item.acId === ac.acId);
  const mutationDetail = item => {
    const exact = item.replacementBefore && item.replacementAfter;
    const changed = exact
      ? `<p>${t('mutation.exactReplacement', `<code>${e(item.replacementBefore)}</code>`, `<code>${e(item.replacementAfter)}</code>`)}</p>`
      : `<p>${t('mutation.reportedDescription', `<code>${e(item.description)}</code>`)}</p><p class="meta">${t('mutation.noExactReplacement')}</p>`;
    const location = [
      item.mutatedClass ? t('mutation.productionClass', `<code>${e(item.mutatedClass)}</code>`) : '',
      item.sourceFile ? t('mutation.sourceFile', `<code>${e(item.sourceFile)}</code>`) : '',
      item.mutatedMethod ? t('mutation.method', `<code>${e(item.mutatedMethod)}</code>`) : '',
      item.methodDescription ? t('mutation.descriptor', `<code>${e(item.methodDescription)}</code>`) : '',
      item.lineNumber != null ? t('mutation.line', `<code>${e(item.lineNumber)}</code>`) : ''
    ].filter(Boolean).map(line => `<li>${line}</li>`).join('');
    const source = item.originalSourceLine
      ? `<h6>${t('mutation.originalLine')}</h6>${code('java', item.originalSourceLine)}`
      : '';
    return `<article class="undetected-mutation" data-mutation-ordinal="${e(item.ordinal)}"><h5>${t('mutation.cardTitle', e(item.ordinal))}</h5><h6>${t('mutation.whatChanged')}</h6>${changed}<h6>${t('mutation.whereChanged')}</h6>${location ? `<ul class="mutation-location">${location}</ul>` : `<p class="meta">${t('mutation.noLocation')}</p>`}${source}<h6>${t('mutation.whatHappened')}</h6><p>${t('mutation.acPassed')}</p></article>`;
  };
  const mutationBody = ac => {
    const safeguard = safeguardFor(ac, 'MUTATION_TESTING');
    const assessment = mutationFor(ac);
    if (!assessment) {
      return `<p>${e(safeguard.verdict === 'DISABLED' || safeguard.verdict === 'INCOMPLETE' ? safeguard.explanation : t('mutation.noAttributed'))}</p>`;
    }
    if (assessment.attributionGap) return `<p>${t('mutation.noAttributed')}</p>`;
    const undetected = ac.undetectedMutations || [];
    return `<p>${t('mutation.summary', e(assessment.coveredMutantCount), e(assessment.killedByAcceptanceMethodMutantCount), e(undetected.length))}</p>${undetected.map(mutationDetail).join('')}`;
  };
  const acCard = ac => {
    const open = ac.status !== 'PASS';
    const statusText = ac.status === 'FAIL' ? t('verification.failedAc') : ac.status === 'NOT_REPORTED' ? t('verification.incompleteAc') : t('verification.passedAc');
    return `<details class="ac-card ${e(ac.status)}" id="verification-${id(ac.acId)}"${open ? ' open' : ''}><summary><span class="ac-id">${e(ac.acId)}</span><strong>${e(ac.title)}</strong> ${badge(ac.status === 'NOT_REPORTED' ? t('acResults.incomplete') : ac.status === 'FAIL' ? t('acResults.failed') : t('acResults.passed'))}</summary><div class="ac-card-body"><p class="ac-result ${e(ac.status)}"><strong>${t('verification.acResult')}:</strong> ${e(statusText)}</p>${safeguardCard(ac, 'PUBLIC_ACCEPTANCE', t('acResults.publicAcceptance'), `<h5>${t('acResults.publicCases')}</h5>${casesMarkup(ac, 'PUBLIC')}`)}${safeguardCard(ac, 'HIDDEN_TESTS', t('acResults.hiddenTests'), `<h5>${t('acResults.hiddenCases')}</h5>${casesMarkup(ac, 'HIDDEN')}`)}${safeguardCard(ac, 'EXPECTED_RESULT_CHECK', t('acResults.expectedResult'))}${safeguardCard(ac, 'PROPERTY_BASED_TESTING', t('acResults.propertyTesting'), propertyResultsFor(ac))}${safeguardCard(ac, 'MUTATION_TESTING', t('acResults.mutationTesting'), `<p>${t('mutation.intro')}</p>${mutationBody(ac)}`)}<details class="ac-technical"><summary>${t('technical.heading')}</summary><p class="meta">${t('technical.description')}</p>${(ac.safeguards || []).map(item => `<p><code>${e(item.technicalGate)}</code>: ${e(item.explanation)}</p>`).join('')}</details></div></details>`;
  };
  const needsAttention = acs => {
    const items = acs.filter(ac => ac.status !== 'PASS');
    return `<section class="problem-summary" id="problems"><h2>${t('problems.heading')}</h2>${items.length ? `<ol>${items.map(ac => `<li><a href="#verification-${id(ac.acId)}">${e(ac.acId)}: ${e(ac.title)}</a> — ${e(ac.status === 'FAIL' ? t('verification.failedAc') : t('verification.incompleteAc'))}</li>`).join('')}</ol>` : `<p>${t('problems.noEvidence')}</p>`}</section>`;
  };
  const technicalMutationEvidence = (value, gateValue) => {
    if (!value) return `<details id="mutation-technical-details"><summary>${t('mutation.technicalDetails')}</summary>${gateCard(gateValue)}</details>`;
    const outcome = rows => !rows?.length ? '' : `<div class="table-wrap"><table><thead><tr><th>${t('mutation.pitStatus')}</th><th>${t('mutation.detected')}</th><th>${t('mutation.mutants')}</th></tr></thead><tbody>${rows.map(row => `<tr><td><code>${e(row.status)}</code></td><td>${e(row.detected)}</td><td>${e(row.count)}</td></tr>`).join('')}</tbody></table></div>`;
    const detected = (value.mutations || []).filter(item => item.detected).length;
    return `<details id="mutation-technical-details"><summary>${t('mutation.technicalDetails')}</summary>${gateCard(gateValue)}<p>${t('mutation.profile', `<code>${e(value.pitVersion)}</code>`, `<code>${e(value.managedProfileId)}</code>`)}</p><h3>${t('mutation.global')}</h3><p>${t('mutation.detectedCount', e(detected))}</p><p class="meta">${t('mutation.globalMeaning')}</p><p>${t('mutation.producerCounts', e(value.producerMutationCount), e(value.uniquelyAttributedMutationCount), e(value.unattributedMutationCount))}</p><h3>${t('mutation.operatorIds')}</h3><ul>${(value.managedOperatorIds || []).map(operator => `<li><code>${e(operator)}</code></li>`).join('')}</ul><h3>${t('mutation.rawOutcomes')}</h3>${outcome(value.producerOutcomeCounts)}<h3>${t('mutation.perAc')}</h3><p class="meta">${t('mutation.perAcMeaning')}</p><div class="table-wrap"><table><thead><tr><th>${t('mutation.ac')}</th><th>${t('mutation.covered')}</th><th>${t('mutation.detectedByMethod')}</th></tr></thead><tbody>${(value.assessments || []).map(assessment => `<tr><td>${e(assessment.acId)}</td><td>${e(assessment.coveredMutantCount)}</td><td>${e(assessment.killedByAcceptanceMethodMutantCount)}</td></tr>`).join('')}</tbody></table></div><h3>${t('mutation.rawFindings')}</h3>${(value.mutations || []).map((item, index) => `<article class="case-card"><h4>${t('mutation.mutant', index + 1)}: <code>${e(item.status)}</code></h4><p>${e(item.description)}</p><p class="meta">${t('mutation.mutator', `<code>${e(item.mutator)}</code>`, e(item.detected))}</p><h5>${t('mutation.coveringTests')}</h5><ul>${(item.coveringTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul><h5>${t('mutation.killingTests')}</h5><ul>${(item.killingTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul><h5>${t('mutation.succeedingTests')}</h5><ul>${(item.succeedingTests || []).map(selector => `<li><code>${e(selector)}</code></li>`).join('')}</ul></article>`).join('')}</details>`;
  };
  const technicalEvidence = () => `<details class="report-section technical-evidence" id="technical-evidence"><summary><h2>${t('technical.heading')}</h2></summary><p>${t('technical.description')}</p><section id="contract-integrity"><h3>${t('verification.contractIntegrity')}</h3>${gateCard(gate('CONTRACT_INTEGRITY'))}</section><section id="public-acceptance"><h3>${t('verification.publicAcceptance')}</h3>${gateCard(gate('JUNIT'))}${gateCard(gate('EXPECTED_CONSUMPTION'))}</section><section id="hidden-tests"><h3>${t('verification.hiddenTests')}</h3>${gateCard(gate('REVIEWER_JUNIT'))}</section><section id="property-testing"><h3>${t('verification.propertyTesting')}</h3>${gateCard(gate('PROPERTY'))}</section><section id="mutation-testing"><h3>${t('verification.mutationTesting')}</h3>${technicalMutationEvidence(data.mutationAttribution, gate('MUTATION'))}</section></details>`;
  const mutationTechnicalCoordinates = () => {
    const details = document.querySelector('#mutation-technical-details');
    const value = data.mutationAttribution;
    if (!details || !value) return;
    const rows = (value.mutations || []).map((item, index) => {
      const location = [
        item.sourceFile ? t('mutation.sourceFile', `<code>${e(item.sourceFile)}</code>`) : '',
        item.mutatedClass ? t('mutation.productionClass', `<code>${e(item.mutatedClass)}</code>`) : '',
        item.mutatedMethod ? t('mutation.method', `<code>${e(item.mutatedMethod)}</code>`) : '',
        item.methodDescription ? t('mutation.descriptor', `<code>${e(item.methodDescription)}</code>`) : '',
        item.lineNumber != null ? t('mutation.line', `<code>${e(item.lineNumber)}</code>`) : '',
        item.block != null ? t('mutation.block', `<code>${e(item.block)}</code>`) : '',
        item.index != null ? t('mutation.bytecodeIndex', `<code>${e(item.index)}</code>`) : ''
      ].filter(Boolean).map(item => `<li>${item}</li>`).join('');
      const source = item.originalSourceLine ? code('java', item.originalSourceLine) : `<p class="meta">${t('mutation.noLocation')}</p>`;
      const attributed = (item.attributedAcceptanceConditionIds || []).map(ac => `<li><code>${e(ac)}</code></li>`).join('') || '<li>—</li>';
      const detected = (item.detectedAcceptanceConditionIds || []).map(ac => `<li><code>${e(ac)}</code></li>`).join('') || '<li>—</li>';
      return `<article class="case-card"><h4>${t('mutation.mutant', index + 1)}</h4>${location ? `<ul class="mutation-location">${location}</ul>` : ''}${source}<h5>${t('mutation.attributedAc')}</h5><ul>${attributed}</ul><h5>${t('mutation.detectedAc')}</h5><ul>${detected}</ul></article>`;
    }).join('');
    const section = document.createElement('section');
    section.innerHTML = `<h3>${t('mutation.rawCoordinates')}</h3>${rows}`;
    details.append(section);
  };
  const verificationPage = () => {
    document.title = t('verification.title'); document.getElementById('title').textContent = t('verification.title');
    const conclusion = data.verdict === 'PASS' ? t('verification.pass') : data.verdict === 'FAIL' ? t('verification.fail') : t('verification.incomplete');
    document.getElementById('notice').textContent = conclusion;
    const run = data.run || {}; const scope = data.deliveryScope || {}; const selected = (scope.acceptanceConditionIds || []).join(', ') || t('verification.fullContract');
    const acs = data.acceptanceConditions || [];
    const passed = run.passedAcceptanceConditionCount ?? acs.filter(ac => ac.status === 'PASS').length;
    const failed = run.failedAcceptanceConditionCount ?? acs.filter(ac => ac.status === 'FAIL').length;
    const incomplete = run.incompleteAcceptanceConditionCount ?? acs.filter(ac => ac.status === 'NOT_REPORTED').length;
    document.getElementById('summary').innerHTML = `<section class="report-intro verification ${e(data.verdict)}"><h2>${e(conclusion)}</h2><p>${t('verification.aggregate', e(acs.length), e(passed), e(failed), e(incomplete))}</p><p class="contract-integrity-summary">${contractIntegritySummary()}</p><p class="meta">${t('verification.run', `<code>${e(run.runId || t('verification.unavailable'))}</code>`, e(run.startedAt || t('verification.unavailable')), e(run.finishedAt || data.generatedAt || t('verification.unavailable')))}</p><p class="meta">${t('verification.scope', e(selected), e(scope.executedHiddenRows ?? 0), e(scope.executedPublicProperties ?? 0))}</p></section><section class="filter-controls" aria-label="${t('verification.filters')}"><label>${t('verification.find')} <input id="case-query" type="search" autocomplete="off"></label>${['FAIL','PASS','NOT_REPORTED'].map(status => `<button type="button" data-status-filter="${status}" aria-pressed="false">${status}</button>`).join('')}</section>${needsAttention(acs)}`;
    const blocked = `<p class="suppressed">${contractIntegritySummary()}</p>`;
    document.getElementById('report').innerHTML = `<section class="report-section" id="all-acs"><h2>${t('verification.allAcs')}</h2>${integrityFailed() ? blocked : acs.map(acCard).join('')}</section>${technicalEvidence()}`;
    document.getElementById('outline').innerHTML = `<h2>${t('verification.outline')}</h2><a href="#problems">${t('verification.needsAttention')}</a><a href="#all-acs">${t('verification.allAcs')}</a><a href="#technical-evidence">${t('technical.heading')}</a>`;
  };
  document.getElementById('skip-link').textContent = t('shell.skipLink');
  document.getElementById('outline').setAttribute('aria-label', t('shell.outline'));
  if (!review && !verification) throw new Error('Unsupported ToppleCat report projection.');
  review ? reviewPage() : verificationPage();
  mutationTechnicalCoordinates();
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
    document.querySelectorAll('.discarded-inputs').forEach(container => {
      const items = [...container.querySelectorAll('[data-discard-item]')]; const size = 25; let page = 0;
      const pageCount = Math.max(1, Math.ceil(items.length / size)); const label = container.querySelector('[data-discard-page]');
      const render = () => { items.forEach((item, index) => { item.hidden = Math.floor(index / size) !== page; }); label.textContent = t('property.page', page + 1, pageCount); container.querySelector('[data-discard-prev]').disabled = page === 0; container.querySelector('[data-discard-next]').disabled = page === pageCount - 1; };
      container.querySelector('[data-discard-prev]').addEventListener('click', () => { if (page > 0) { page -= 1; render(); } });
      container.querySelector('[data-discard-next]').addEventListener('click', () => { if (page + 1 < pageCount) { page += 1; render(); } });
      render();
    });
  }
  document.querySelectorAll('.mermaid-diagram').forEach(container => {
    const source = container.querySelector('.mermaid-source')?.textContent || '';
    try { container.innerHTML = window.ToppleCatMermaid.render(source); } catch (_error) { container.innerHTML = `<p class="mermaid-error">${t('mermaid.error')}</p>`; }
  });
})();
