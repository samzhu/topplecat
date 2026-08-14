(() => {
  const data = JSON.parse(document.getElementById('topplecat-report-data').textContent);
  const presentation = JSON.parse(document.getElementById('topplecat-report-presentation').textContent);
  const catalogs = {
    en: {
      'property.discarded': 'Discarded generator inputs', 'property.discardedReason': "This input did not meet this run's generated-input range.", 'property.previous': 'Previous page', 'property.next': 'Next page', 'property.page': 'Page {0} of {1}',
      'mutation.detectedAll': 'Every attributed altered program made the public acceptance fail as expected.', 'mutation.survived': '{0} altered programs failed as expected, but {1} still passed. The public acceptance did not find those simulated errors, so this function may have a problem that the current acceptance does not reveal.', 'mutation.noAttributed': 'No mutation was exactly attributed to this AC in the current run.',
      'technical.heading': 'Technical evidence', 'technical.description': 'Canonical Gate names, run metadata, and producer details remain here for audit. They do not replace the AC explanations above.', 'verification.needsAttention': 'Needs attention', 'verification.allAcs': 'All ACs', 'verification.acResult': 'Verification result', 'verification.incompleteAc': 'This AC could not be fully assessed.', 'verification.failedAc': 'Verification found a problem for this AC.', 'verification.passedAc': 'Verification passed for this AC.', 'verification.readingToolbar': 'AC reading controls', 'verification.expandAll': 'Expand all ACs', 'verification.collapseAll': 'All ACs: key results only', 'verification.expandThis': 'Expand this AC', 'verification.collapseThis': 'This AC: key result only', 'verification.stopBulk': 'Stop and show key results', 'verification.expanding': 'Expanding {0} of {1}', 'verification.expandedAll': 'All AC reader details are open.', 'verification.keyResultsOnly': 'All ACs show key results only.', 'verification.expansionInterrupted': 'Expansion stopped after {0} of {1} ACs.', 'verification.readerDetails': 'Reader details for {0}',
      'phase.GIVEN': 'Given', 'phase.WHEN': 'When', 'phase.THEN': 'Then', 'phase.AND': 'And',
      'scenario.unavailable': 'Scenario source is unavailable.', 'scenario.aria': 'Given When Then scenario',
      'image.openRemote': 'Open remote image', 'image.unavailable': 'Image unavailable.', 'image.noAlt': 'No alternative text was authored.',
      'mermaid.source': 'View Mermaid source', 'mermaid.error': 'Diagram could not be rendered. The escaped original Mermaid source is available below.',
      'markdown.unrenderable': 'Content could not be rendered as Markdown. Its escaped source is preserved below.',
      'visibility.hidden': 'Reviewer case', 'visibility.public': 'Public case',
      'method.source': 'Acceptance Method source', 'method.description': 'Only the AC-bound acceptance method is shown. Stage, helper, and production source are excluded.', 'method.unavailable': 'Acceptance Method source is unavailable.',
      'inputs': 'Inputs', 'expectedResult': 'Expected result', 'case.valuesSummary': 'Show complete input and expected values', 'common.none': 'None',
      'property.declarations': 'Property declarations', 'property.tries': '{0} tries, at most {1} discards and {2} shrinks.', 'property.details': 'Property source and technical details',
      'advisory.heading': 'Contract Quality Advisories', 'advisory.description': 'These reviewer-only observations are non-blocking. They do not add a business rule, change an execution result, or alter a Gate.', 'advisory.for': 'for', 'advisory.publicRows': 'Public rows: {0}.', 'advisory.reviewerRows': 'Reviewer rows: {0}.',
      'review.selectedSdd': 'Selected SDD', 'review.documentPosition': 'document position {0}.', 'review.noSpec': 'No external Spec document was selected for this full-contract review.', 'review.material': 'Executable Scenario and typed cases', 'review.projection': 'ToppleCat projection', 'review.projectionDescription': 'The checked Java/JUnit Acceptance Method, compiler-described Scenarios, Typed Case Rows, and applicable Properties are inserted at this ID-bearing marker. This projection adds no business rule.', 'review.noRows': 'No typed case rows were recorded.', 'review.techSummary': 'Technical and policy metadata', 'review.techMeta': 'This report projects the checked executable contract. Its details do not add a rule or execution result.',
      'review.title': 'Spec Review', 'review.notice': 'Specification prepared, not executed', 'review.withDocs': 'The complete selected SDD documents appear once. The checked Java/JUnit contract is inserted at each declared acceptance marker.', 'review.withoutDocs': 'No external Spec document was selected. This is the complete executable contract, not an invented Markdown document.', 'review.selectedDocuments': 'Selected SDD documents', 'review.outline': 'On this page', 'review.advisories': 'Advisories',
      'gate.evidenceUnavailable': 'Current-run evidence is unavailable.', 'gate.canonicalReason': '{0}',
      'comparison.heading': 'Expected compared with actual', 'comparison.scope': 'Only fields actually compared by the acceptance code appear here. ToppleCat cannot conclude that a rule was checked when it is not represented in this comparison.', 'comparison.boundTo': 'Comparison recorded by Step', 'comparison.expected': 'Expected', 'comparison.path': 'Checked field', 'comparison.difference': 'Difference type', 'comparison.actual': 'Actual', 'comparison.kind.CHANGED': 'Values differ', 'comparison.kind.MISSING_EXPECTED': 'Expected field is missing from actual result', 'comparison.kind.UNEXPECTED_ACTUAL': 'Actual result contains an unexpected field',
      'stepData.heading': 'Values passed to Steps', 'stepData.arguments': 'Technical values passed to',
      'case.publicRule': 'Acceptance Condition', 'case.scenario': 'Scenario and execution Steps', 'case.executionDetails': 'Show Scenario and execution details', 'case.failedStep': 'Failed or last reached Step', 'case.noStep': 'No Scenario Step was reached.', 'case.completeExpected': 'Show the complete expected result', 'case.rawFailure': 'Show raw failure and technical metadata', 'case.noRawFailure': 'No raw failure was recorded.', 'case.expectedConsumption': 'Expected consumption', 'case.noCurrentRows': 'No current rows were available for this area.',
      'property.none': 'No Property declaration applied to this scope.', 'property.results': '{0} of {1} requested generated inputs completed; discarded inputs: {2}.', 'property.failed': 'A generated input violated this Property, so this check stopped early.', 'property.original': 'Original counterexample', 'property.shrunk': 'Simplified counterexample', 'property.replay': 'Replay token', 'property.discarded': 'Discarded generator inputs', 'property.discardedReason': "This input did not meet this run's generated-input range.", 'property.previous': 'Previous page', 'property.next': 'Next page', 'property.page': 'Page {0} of {1}',
      'acResults.publicAcceptance': 'Public Acceptance', 'acResults.hiddenTests': 'Hidden Tests', 'acResults.expectedResult': 'Expected Result Check', 'acResults.propertyTesting': 'Property-Based Testing', 'acResults.mutationTesting': 'Mutation Testing', 'acResults.passed': 'Passed', 'acResults.failed': 'Problem found', 'acResults.incomplete': 'Unable to assess', 'acResults.disabled': 'Disabled', 'acResults.notApplicable': 'Not applicable', 'acResults.comparisonCompleted': 'Comparison completed', 'acResults.recordedReason': 'Recorded reason: {0}', 'acResults.publicCases': 'Public Acceptance cases', 'acResults.hiddenCases': 'Hidden Test cases',
      'safeguard.reason.CASE_FAILED': 'At least one recorded case produced a result different from the acceptance expectation.', 'safeguard.reason.ALL_CASES_PASSED': 'All recorded cases produced the accepted result.', 'safeguard.reason.NO_CASE_EVIDENCE': 'No current-run case result was available for this AC.', 'safeguard.reason.EXPECTED_COMPARISON_MISSING': 'The run did not record whether the authored expected result was compared with the actual result.', 'safeguard.reason.EXPECTED_NOT_COMPARED': 'An authored expected result was available, but the acceptance code did not compare it with the actual result.', 'safeguard.reason.EXPECTED_COMPARISON_COMPLETED': 'All authored expected results were compared with actual results. This means the comparisons ran, not that the values matched. See Public Acceptance and the differences below for equality.', 'safeguard.reason.NO_EXPECTED_RESULT': 'This AC has no authored expected result to compare.', 'safeguard.reason.PROPERTY_COUNTEREXAMPLE': 'A generated input violated the authored Property rule.', 'safeguard.reason.PROPERTY_EVIDENCE_INCOMPLETE': 'Property-Based Testing did not produce complete current-run evidence.', 'safeguard.reason.PROPERTY_COMPLETED': 'All completed generated inputs satisfied the authored Property rule.', 'safeguard.reason.NO_PROPERTY': 'This AC has no Property declaration in the current scope.', 'safeguard.reason.MUTATION_BASELINE_FAILED': 'Public Acceptance already found a problem in the original program. Without a passing baseline, this run cannot tell whether the same acceptance would detect a simulated fault.', 'safeguard.reason.MUTATION_EVIDENCE_UNAVAILABLE': 'The current run did not provide a trustworthy basis for assessing this AC with Mutation Testing.', 'safeguard.reason.NO_MUTATION_ATTRIBUTED': 'No mutation was exactly attributed to this AC in the current run.', 'safeguard.reason.MUTATION_ATTRIBUTION_GAP': 'No mutation was exactly attributed to this Acceptance Method in the current run.', 'safeguard.reason.MUTATION_SURVIVED': "At least one attributed altered program still passed this AC's public acceptance.", 'safeguard.reason.MUTATION_DETECTED': "Every attributed altered program made this AC's public acceptance fail as expected.", 'safeguard.reason.GATE_RECORDED': 'This safeguard cannot be assessed from trustworthy current-run evidence.',
      'mutation.intro': "Mutation Testing evaluates whether the public acceptance can detect simulated faults in production code. ToppleCat keeps this AC's public acceptance unchanged, alters only production code, and runs the acceptance again.", 'mutation.detectedAll': 'Every attributed altered program made the public acceptance fail as expected.', 'mutation.survived': '{0} altered programs failed as expected, but {1} still passed. The public acceptance did not find those simulated errors, so this function may have a problem that the current acceptance does not reveal.', 'mutation.noAttributed': 'No mutation was exactly attributed to this AC in the current run.', 'mutation.technicalDetails': 'Mutation technical evidence', 'mutation.pitStatus': 'PIT status', 'mutation.detected': 'Detected', 'mutation.mutants': 'Mutants', 'mutation.profile': 'PIT {0}, managed profile {1}.', 'mutation.global': 'PIT producer outcome', 'mutation.detectedCount': '{0} producer mutations were detected by at least one test.', 'mutation.globalMeaning': 'This PIT-wide observation does not mean that every Acceptance Method detected every mutation.', 'mutation.producerCounts': '{0} producer mutations. {1} uniquely attributed. {2} unattributed.', 'mutation.operatorIds': 'Managed operator IDs', 'mutation.perAc': 'Per-AC attributed mutation assessment', 'mutation.perAcMeaning': 'Each selected AC is assessed only through its own public Acceptance Method. A different AC cannot supply detection credit.', 'mutation.ac': 'AC', 'mutation.covered': 'Attributed', 'mutation.detectedByMethod': 'Failed as expected', 'mutation.rawOutcomes': 'Raw producer outcomes', 'mutation.rawFindings': 'Raw PIT findings', 'mutation.mutant': 'Mutation {0}', 'mutation.mutator': 'Mutator {0}. Detected: {1}.', 'mutation.coveringTests': 'coveringTests', 'mutation.killingTests': 'killingTests', 'mutation.succeedingTests': 'succeedingTests',
      'technical.heading': 'Technical evidence', 'technical.description': 'Canonical Gate names, run metadata, and producer details remain here for audit. They do not replace the AC explanations above.', 'problems.heading': 'Needs attention', 'problems.noEvidence': 'This run did not provide enough information to complete verification.', 'verification.title': 'Verification Report', 'verification.pass': 'Verification passed', 'verification.fail': 'Verification found a problem', 'verification.incomplete': 'Verification incomplete', 'verification.aggregate': '{0} selected ACs: {1} passed, {2} have a problem, {3} could not be fully assessed.', 'verification.needsAttention': 'Needs attention', 'verification.allAcs': 'All ACs', 'verification.acResult': 'Verification result', 'verification.incompleteAc': 'This AC could not be fully assessed.', 'verification.failedAc': 'Verification found a problem for this AC.', 'verification.passedAc': 'Verification passed for this AC.', 'verification.unavailable': 'unavailable', 'verification.run': 'Run ID: {0}. Started: {1}. Finished: {2}.', 'verification.fullContract': 'Full executable contract', 'verification.scopeFromSpec': 'Selected from Spec documents', 'verification.scopeFromAc': 'Selected by explicit AC IDs', 'verification.scope': '{0}: {1}. Hidden rows: {2}. Properties: {3}.', 'verification.scopedPass': 'This PASS covers only the selected ACs; it does not mean the complete executable contract passed.', 'verification.filters': 'Verification report filters', 'verification.find': 'Find AC or case', 'verification.contractMatchesSeal': 'Contract Integrity passed: the complete executable contract matches its Mechanical Seal.', 'verification.contractMismatch': 'Contract Integrity failed: the complete executable contract no longer matches its Mechanical Seal, so downstream AC work did not run.', 'verification.contractUnverified': 'Contract Integrity lacks trustworthy current-run evidence, so downstream AC work did not run.', 'verification.contractIntegrity': 'Contract Integrity', 'verification.publicAcceptance': 'Public Acceptance', 'verification.hiddenTests': 'Hidden Tests', 'verification.propertyTesting': 'Property-Based Testing', 'verification.mutationTesting': 'Mutation Testing', 'verification.outline': 'Verification',
      'shell.skipLink': 'Skip to report content', 'shell.outline': 'Report outline'
    },
    'zh-TW': {
      'property.discarded': '被捨棄的產生器輸入', 'property.discardedReason': '這個輸入不符合本次執行的產生輸入範圍。', 'property.previous': '上一頁', 'property.next': '下一頁', 'property.page': '第 {0} 頁，共 {1} 頁',
      'mutation.detectedAll': '每個歸因到的改動程式都讓公開驗收如預期失敗。', 'mutation.survived': '{0} 個改動程式如預期失敗，但仍有 {1} 個通過。公開驗收沒有找到那些模擬錯誤，因此這個函式可能存在目前驗收無法揭露的問題。', 'mutation.noAttributed': '本次執行沒有突變被精確歸因到這個 AC。',
      'technical.heading': '技術證據', 'technical.description': 'Canonical Gate 名稱、執行中繼資料與 producer 細節保留在此供稽核；它們不取代上方的 AC 說明。', 'verification.needsAttention': '需要注意', 'verification.allAcs': '所有 AC', 'verification.acResult': '驗證結果', 'verification.incompleteAc': '這個 AC 無法完成評估。', 'verification.failedAc': '這個 AC 發現問題。', 'verification.passedAc': '這個 AC 驗證通過。', 'verification.readingToolbar': 'AC 閱讀控制', 'verification.expandAll': '展開所有 AC', 'verification.collapseAll': '所有 AC：只看關鍵結果', 'verification.expandThis': '展開這個 AC', 'verification.collapseThis': '這個 AC：只看關鍵結果', 'verification.stopBulk': '停止並顯示關鍵結果', 'verification.expanding': '正在展開 {0}／{1}', 'verification.expandedAll': '所有 AC 的閱讀細節都已展開。', 'verification.keyResultsOnly': '所有 AC 都只顯示關鍵結果。', 'verification.expansionInterrupted': '已在 {0}／{1} 個 AC 後停止展開。', 'verification.readerDetails': '{0} 的閱讀細節',
      'verification.pass': '驗證通過', 'verification.fail': '驗證發現問題', 'verification.incomplete': '驗證資料不完整', 'verification.aggregate': '{0} 個選定 AC：{1} 個通過、{2} 個發現問題、{3} 個無法完成評估。',
      'phase.GIVEN': '假設', 'phase.WHEN': '當', 'phase.THEN': '那麼', 'phase.AND': '且',
      'scenario.unavailable': '情境來源無法使用。', 'scenario.aria': '假設、當、那麼情境',
      'image.openRemote': '開啟遠端圖片', 'image.unavailable': '圖片無法使用。', 'image.noAlt': '沒有撰寫替代文字。',
      'mermaid.source': '查看 Mermaid 原始碼', 'mermaid.error': '無法繪製圖表。下方保留了已跳脫的原始 Mermaid 原始碼。',
      'markdown.unrenderable': '無法將內容算繪為 Markdown。下方保留了已跳脫的原始內容。',
      'visibility.hidden': '審閱者案例', 'visibility.public': '公開案例',
      'method.source': '驗收方法原始碼', 'method.description': '此處只顯示綁定 AC 的驗收方法；不會顯示 Stage、輔助程式或產品原始碼。', 'method.unavailable': '驗收方法原始碼無法使用。',
      'inputs': '輸入', 'expectedResult': '預期結果', 'case.valuesSummary': '查看完整輸入與預期值', 'common.none': '無',
      'property.declarations': 'Property 宣告', 'property.tries': '{0} 次嘗試，最多 {1} 次捨棄與 {2} 次縮減。', 'property.details': 'Property 原始碼與技術細節',
      'advisory.heading': '契約品質提醒', 'advisory.description': '這些僅供審閱者閱讀的觀察不會阻擋流程。它們不會新增業務規則、改變執行結果或修改 Gate。', 'advisory.for': '適用於', 'advisory.publicRows': '公開資料列：{0}。', 'advisory.reviewerRows': '審閱者資料列：{0}。',
      'review.selectedSdd': '已選 SDD', 'review.documentPosition': '文件位置 {0}。', 'review.noSpec': '這次完整契約審閱沒有選擇外部 Spec 文件。', 'review.material': '可執行的情境與型別案例', 'review.projection': 'ToppleCat 投影', 'review.projectionDescription': '已檢查的 Java/JUnit 驗收方法、編譯器描述的情境、型別案例資料列與適用的 Property 會插入這個宣告的載入點。這個投影不會新增業務規則。', 'review.noRows': '沒有記錄型別案例資料列。', 'review.techSummary': '技術與政策中繼資料', 'review.techMeta': '本報告投影已檢查的可執行契約；其中細節不會新增規則或執行結果。',
      'review.title': '規格審閱', 'review.notice': '規格已備妥，尚未執行', 'review.withDocs': '完整的已選 SDD 文件只呈現一次；已檢查的 Java/JUnit 契約會插入每個帶 ID marker。', 'review.withoutDocs': '沒有選擇外部 Spec 文件。這是完整的可執行契約，不是 ToppleCat 臆造的 Markdown 文件。', 'review.selectedDocuments': '已選 SDD 文件', 'review.outline': '本頁內容', 'review.advisories': '提醒',
      'gate.evidenceUnavailable': '本次執行證據無法使用。', 'gate.canonicalReason': '本次執行證據記錄的原始原因：{0}',
      'comparison.heading': '預期與實際的比對結果', 'comparison.scope': '這裡只列出驗收程式實際比對的欄位。未出現在比對中的規則，ToppleCat 無法判定是否已檢查。', 'comparison.boundTo': '由下列步驟記錄比對', 'comparison.expected': '預期', 'comparison.path': '檢查欄位', 'comparison.difference': '差異類型', 'comparison.actual': '實際', 'comparison.kind.CHANGED': '兩邊的值不同', 'comparison.kind.MISSING_EXPECTED': '實際結果缺少預期欄位', 'comparison.kind.UNEXPECTED_ACTUAL': '實際結果多出未預期欄位',
      'stepData.heading': '傳入步驟的值', 'stepData.arguments': '傳入下列步驟的技術資料：',
      'case.publicRule': '驗收條件', 'case.scenario': '情境與執行步驟', 'case.executionDetails': '查看情境與執行細節', 'case.failedStep': '失敗或最後到達的步驟', 'case.noStep': '沒有到達任何情境步驟。', 'case.completeExpected': '查看完整預期結果', 'case.rawFailure': '查看原始失敗與技術中繼資料', 'case.noRawFailure': '沒有記錄原始失敗。', 'case.expectedConsumption': '預期值使用狀態', 'case.noCurrentRows': '此區域沒有可用的本次資料列。',
      'property.none': '此範圍沒有適用的 Property 宣告。', 'property.results': '已完成 {0}／{1} 個要求的產生輸入；被捨棄的輸入：{2} 個。', 'property.failed': '有一個產生輸入違反此 Property，因此本次檢查提早停止。', 'property.original': '原始反例', 'property.shrunk': '簡化後反例', 'property.replay': '重播權杖', 'property.discarded': '被捨棄的產生器輸入', 'property.discardedReason': '這個輸入不符合本次執行的產生輸入範圍。', 'property.previous': '上一頁', 'property.next': '下一頁', 'property.page': '第 {0} 頁，共 {1} 頁',
      'acResults.publicAcceptance': '公開驗收', 'acResults.hiddenTests': '隱藏測試', 'acResults.expectedResult': '預期結果檢查', 'acResults.propertyTesting': '性質導向測試', 'acResults.mutationTesting': '突變測試', 'acResults.passed': '通過', 'acResults.failed': '發現問題', 'acResults.incomplete': '無法評估', 'acResults.disabled': '已停用', 'acResults.notApplicable': '不適用', 'acResults.comparisonCompleted': '已完成比對', 'acResults.recordedReason': '記錄原因：{0}', 'acResults.publicCases': '公開驗收案例', 'acResults.hiddenCases': '隱藏測試案例',
      'safeguard.reason.CASE_FAILED': '至少一個案例的實際結果不符合驗收預期。', 'safeguard.reason.ALL_CASES_PASSED': '所有已記錄案例的結果都符合驗收預期。', 'safeguard.reason.NO_CASE_EVIDENCE': '本次執行沒有這個 AC 的案例結果。', 'safeguard.reason.EXPECTED_COMPARISON_MISSING': '本次執行未記錄已撰寫的預期結果是否與實際結果進行比對。', 'safeguard.reason.EXPECTED_NOT_COMPARED': '驗收程式取得了已撰寫的預期結果，但沒有拿它與實際結果比對。', 'safeguard.reason.EXPECTED_COMPARISON_COMPLETED': '所有已撰寫的預期結果都已與實際結果比對。這只表示有執行比對，不代表兩者相同；是否相同請看「公開驗收」與下方差異。', 'safeguard.reason.NO_EXPECTED_RESULT': '這個 AC 沒有已撰寫的預期結果可供比對。', 'safeguard.reason.PROPERTY_COUNTEREXAMPLE': '有一個產生的輸入違反已撰寫的 Property 規則。', 'safeguard.reason.PROPERTY_EVIDENCE_INCOMPLETE': '性質導向測試沒有產生完整的本次執行證據。', 'safeguard.reason.PROPERTY_COMPLETED': '所有已完成的產生輸入都符合已撰寫的 Property 規則。', 'safeguard.reason.NO_PROPERTY': '這個 AC 在目前範圍內沒有 Property 宣告。', 'safeguard.reason.MUTATION_BASELINE_FAILED': '原始程式的公開驗收已經發現問題。缺少通過的比較基準，本次無法判斷同一項驗收能否辨識模擬錯誤。', 'safeguard.reason.MUTATION_EVIDENCE_UNAVAILABLE': '本次執行沒有提供可用來評估這個 AC 的可信突變測試基礎。', 'safeguard.reason.NO_MUTATION_ATTRIBUTED': '本次執行沒有突變被精確歸因到這個 AC。', 'safeguard.reason.MUTATION_ATTRIBUTION_GAP': '本次執行沒有突變被精確歸因到這個驗收方法。', 'safeguard.reason.MUTATION_SURVIVED': '至少一個歸因到這個 AC 的改動程式仍通過公開驗收。', 'safeguard.reason.MUTATION_DETECTED': '每個歸因到這個 AC 的改動程式都讓公開驗收如預期失敗。', 'safeguard.reason.GATE_RECORDED': '依據可信的本次執行證據，目前無法評估這項防護。',
      'mutation.intro': '突變測試用於評估公開驗收能否辨識正式程式中的模擬錯誤。ToppleCat 會保持這個 AC 的公開驗收不變，只改動正式程式後重新執行驗收。', 'mutation.technicalDetails': '突變測試技術證據', 'mutation.pitStatus': 'PIT 狀態', 'mutation.detected': '已偵測', 'mutation.mutants': '突變體', 'mutation.profile': 'PIT {0}，受管理設定檔 {1}。', 'mutation.global': 'PIT producer 結果', 'mutation.detectedCount': '{0} 個 producer 突變體至少被一項測試偵測到。', 'mutation.globalMeaning': '這個 PIT 全域觀察不代表每個驗收方法都偵測到每個突變體。', 'mutation.producerCounts': '{0} 個 producer 突變體；{1} 個獲得唯一歸因；{2} 個未歸因。', 'mutation.operatorIds': '受管理運算子 ID', 'mutation.rawOutcomes': '原始 producer 結果', 'mutation.perAc': '每個 AC 的歸因突變評估', 'mutation.perAcMeaning': '每個選定 AC 只透過自己的公開驗收方法評估；其他 AC 不能提供偵測信用。', 'mutation.ac': 'AC', 'mutation.covered': '已歸因', 'mutation.detectedByMethod': '如預期失敗', 'mutation.rawFindings': '原始 PIT 發現', 'mutation.mutant': '突變體 {0}', 'mutation.mutator': 'Mutator {0}。已偵測：{1}。', 'mutation.coveringTests': 'coveringTests', 'mutation.killingTests': 'killingTests', 'mutation.succeedingTests': 'succeedingTests',
      'problems.defaultGate': '這個 Gate 在這次執行記錄為 {0}。', 'problems.hiddenCase': '這個審閱者案例在執行情境時發現問題。', 'problems.publicCase': '這個公開案例在執行情境時發現問題。', 'problems.heading': '問題摘要', 'problems.noEvidence': '本次執行沒有足夠資料完成驗證。',
      'verification.title': '驗證報告', 'verification.pass': '驗證通過', 'verification.fail': '驗證發現問題', 'verification.incomplete': '驗證資料不完整', 'verification.aggregate': '{0} 個選定 AC：{1} 個通過、{2} 個發現問題、{3} 個無法完成評估。', 'verification.unavailable': '無法使用', 'verification.run': '執行 ID：{0}。開始：{1}。完成：{2}。', 'verification.fullContract': '完整可執行契約', 'verification.scopeFromSpec': '由 Spec 文件選取', 'verification.scopeFromAc': '由明確 AC ID 選取', 'verification.scope': '{0}：{1}。隱藏資料列：{2}。Property：{3}。', 'verification.scopedPass': '這個 PASS 只代表選定的 AC 通過，不代表完整可執行契約通過。', 'verification.filters': '驗證報告篩選器', 'verification.find': '尋找 AC 或案例', 'verification.contractMatchesSeal': '契約完整性已通過：完整可執行契約符合機械封印。', 'verification.contractMismatch': '契約完整性失敗：完整可執行契約已不符合機械封印，因此未執行下游 AC 工作。', 'verification.contractUnverified': '契約完整性缺少可信的本次執行證據，因此未執行下游 AC 工作。', 'verification.contractIntegrity': '契約完整性', 'verification.publicAcceptance': '公開驗收', 'verification.hiddenTests': '隱藏測試', 'verification.propertyTesting': '性質導向測試', 'verification.mutationTesting': '突變測試', 'verification.outline': '驗證',
      'shell.skipLink': '跳至報告內容', 'shell.outline': '報告大綱'
    }
  };
  const mutationDetailMessages = {
    en: {
      'info.mutationTesting.term': 'Mutation Testing',
      'info.mutationTesting.aria': 'More about Mutation Testing',
      'info.mutationTesting.description': "ToppleCat temporarily simulates small changes to production code and reruns this AC's unchanged public acceptance. A missed change does not prove the original production code is wrong.",
      'info.attributedChanges.term': 'attributed changes',
      'info.attributedChanges.aria': 'More about attributed changes',
      'info.attributedChanges.description': 'Only simulated changes exactly associated with this AC\'s public Acceptance Method count here. Another AC detecting a change does not give this AC detection credit.',
      'info.undetectedMutation.term': 'Undetected mutation',
      'info.undetectedMutation.aria': 'More about an undetected mutation',
      'info.undetectedMutation.description': "This simulated change still passed this AC's unchanged public acceptance.",
      'info.originalSourceLine.term': 'Original source line',
      'info.originalSourceLine.aria': 'More about the original source line',
      'info.originalSourceLine.description': 'This is the original production line used to locate the relevant logic, not necessarily the changed program text.',
      'info.descriptor.term': 'Descriptor',
      'info.descriptor.aria': 'More about the descriptor',
      'info.descriptor.description': 'A JVM method signature used for exact technical location. Ordinary reading does not require it.',
      'mutation.summary': 'This AC was assessed against {0} {1}: {2} detected, {3} undetected.',
      'mutation.cardTitle': '{0} {1}',
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
      'info.mutationTesting.term': '突變測試',
      'info.mutationTesting.aria': '更多關於突變測試',
      'info.mutationTesting.description': 'ToppleCat 會暫時模擬正式程式的小幅改動，重新執行這個 AC 未改變的公開驗收。沒有偵測到某個改動，不能因此證明原始正式程式有錯。',
      'info.attributedChanges.term': '已歸因改動',
      'info.attributedChanges.aria': '更多關於已歸因改動',
      'info.attributedChanges.description': '這裡只計入精確關聯到這個 AC 公開驗收方法的模擬改動。其他 AC 偵測到改動，也不會讓這個 AC 取得偵測信用。',
      'info.undetectedMutation.term': '未偵測到的突變',
      'info.undetectedMutation.aria': '更多關於未偵測到的突變',
      'info.undetectedMutation.description': '這個模擬改動仍然通過了這個 AC 未改變的公開驗收。',
      'info.originalSourceLine.term': '原始碼行',
      'info.originalSourceLine.aria': '更多關於原始碼行',
      'info.originalSourceLine.description': '這是用來定位相關邏輯的原始正式程式碼行，不一定是改動後的程式文字。',
      'info.descriptor.term': '描述子',
      'info.descriptor.aria': '更多關於描述子',
      'info.descriptor.description': '用於精確技術定位的 JVM 方法簽名。一般閱讀不需要理解它。',
      'mutation.summary': '這個 AC 共評估 {0} 個{1}：偵測到 {2} 個，未偵測到 {3} 個。',
      'mutation.cardTitle': '{0} {1}',
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
  let informationOrdinal = 0;
  const informationControl = key => {
    const ordinal = ++informationOrdinal;
    const popoverId = `topplecat-info-${id(key)}-${ordinal}`;
    return `<span class="info-term" data-info-wrapper data-info-key="${e(key)}"><span class="info-term-label">${e(t(`info.${key}.term`))}</span><button type="button" class="info-button" data-info-button aria-expanded="false" aria-controls="${popoverId}" aria-describedby="${popoverId}" aria-label="${e(t(`info.${key}.aria`))}">ⓘ</button><span class="info-popover" data-info-popover id="${popoverId}" role="tooltip" hidden>${e(t(`info.${key}.description`))}</span></span>`;
  };
  const linkKnownReferences = value => value.replace(/(?<![A-Za-z0-9_-])(AC-[A-Za-z0-9][A-Za-z0-9-]*)(?![A-Za-z0-9_-])/g, (match) => {
      const known = (data.acceptanceConditions || []).some(item => item.acId === match);
      return known ? `<a class="ac-reference" href="#review-${id(match)}">${match}</a>` : match;
    });
  const closingBracket = (source, start) => {
    let escaped = false;
    for (let index = start; index < source.length; index += 1) {
      const character = source[index];
      if (escaped) { escaped = false; continue; }
      if (character === '\\') { escaped = true; continue; }
      if (character === ']') return index;
    }
    return -1;
  };
  const authoredLink = (source, start) => {
    if (source[start] !== '[') return null;
    const labelEnd = closingBracket(source, start + 1);
    if (labelEnd < 0 || source[labelEnd + 1] !== '(') return null;
    let index = labelEnd + 2;
    while (/\s/.test(source[index] || '')) index += 1;
    let destination = '';
    if (source[index] === '<') {
      const end = source.indexOf('>', index + 1);
      if (end < 0) return null;
      destination = source.slice(index + 1, end); index = end + 1;
    } else {
      const destinationStart = index; let depth = 0; let escaped = false;
      while (index < source.length) {
        const character = source[index];
        if (escaped) { escaped = false; index += 1; continue; }
        if (character === '\\') { escaped = true; index += 1; continue; }
        if (character === '(') depth += 1;
        if (character === ')') { if (depth === 0) break; depth -= 1; }
        if (depth === 0 && /\s/.test(character)) break;
        index += 1;
      }
      destination = source.slice(destinationStart, index);
    }
    while (/\s/.test(source[index] || '')) index += 1;
    let title = '';
    if (source[index] === '"' || source[index] === "'") {
      const quote = source[index]; const titleStart = ++index; let escaped = false;
      while (index < source.length) {
        if (escaped) { escaped = false; index += 1; continue; }
        if (source[index] === '\\') { escaped = true; index += 1; continue; }
        if (source[index] === quote) break;
        index += 1;
      }
      if (source[index] !== quote) return null;
      title = source.slice(titleStart, index); index += 1;
      while (/\s/.test(source[index] || '')) index += 1;
    }
    if (source[index] !== ')') return null;
    return { end: index + 1, label: source.slice(start + 1, labelEnd), destination, title };
  };
  const inline = source => {
    const protectedParts = [];
    const protect = html => {
      const token = `\u0000${protectedParts.length}\u0000`;
      protectedParts.push(html);
      return token;
    };
    let value = ''; let position = 0;
    while (position < String(source).length) {
      const text = String(source);
      if (text[position] === '`') {
        let run = 1; while (text[position + run] === '`') run += 1;
        const delimiter = '`'.repeat(run); const end = text.indexOf(delimiter, position + run);
        if (end >= 0) {
          value += protect(`<code>${e(text.slice(position + run, end))}</code>`);
          position = end + run; continue;
        }
      }
      if (text[position] === '[') {
        const parsed = authoredLink(text, position);
        if (parsed) {
          const safe = safeHref(parsed.destination);
          const label = e(parsed.label);
          const rendered = safe
            ? `<a href="${e(safe)}"${parsed.title ? ` title="${e(parsed.title)}"` : ''} target="_blank" rel="noopener">${label}</a>`
            : `${label} <code>${e(parsed.destination)}</code>`;
          value += protect(rendered); position = parsed.end; continue;
        }
      }
      value += e(text[position]); position += 1;
    }
    value = linkKnownReferences(value);
    value = value
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>')
      .replace(/_([^_]+)_/g, '<em>$1</em>');
    return value.replace(/\u0000(\d+)\u0000/g, (_all, index) => protectedParts[Number(index)] ?? '');
  };
  const badge = value => `<span class="badge ${e(value)}">${e(value)}</span>`;
  const statusBadge = (status, label) => `<span class="badge ${e(status)}">${e(label)}</span>`;
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
    const children = block.children || [];
    const renderChildren = () => children.map(markdownBlock).join('');
    const legacyItems = (block.items || []).map(item => `<li>${inline(item)}</li>`).join('');
    const legacyTaskItems = (block.items || []).map(item => { const checked = /^\[x]/i.test(item); return `<li><input type="checkbox" disabled ${checked ? 'checked' : ''}>${inline(item.replace(/^\[[ xX]]\s*/, ''))}</li>`; }).join('');
    const listItems = children.length ? renderChildren() : block.kind === 'TASK_LIST' ? legacyTaskItems : legacyItems;
    switch (block.kind) {
      case 'HEADING': { const level = Math.min(Math.max((block.headingLevel || 1) + 1, 2), 6); return `<h${level}${anchor}>${inline(block.text)}</h${level}>`; }
      case 'PARAGRAPH': return `<p${anchor}>${inline(block.text)}</p>`;
      case 'LIST': return `<ul${anchor}>${listItems}</ul>`;
      case 'ORDERED_LIST': return `<ol${anchor}>${listItems}</ol>`;
      case 'TASK_LIST': return `<ul class="task-list"${anchor}>${listItems}</ul>`;
      case 'LIST_ITEM': {
        const marker = /^\[[ xX]\]$/.test(String(block.text || '')) ? String(block.text).toLowerCase() : '';
        const checkbox = marker ? `<input type="checkbox" disabled ${marker === '[x]' ? 'checked' : ''}>` : '';
        return `<li>${checkbox}${children.length ? renderChildren() : inline(block.text || '')}</li>`;
      }
      case 'BLOCK_QUOTE': return `<blockquote${anchor}>${children.length ? renderChildren() : inline(block.text || '')}</blockquote>`;
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
      case 'ACCEPTANCE_MARKER': {
        const condition = (data.acceptanceConditions || []).find(item => item.acId === block.anchorId);
        return condition ? reviewAc(condition, true) : '';
      }
      default: return `<section${anchor}><p class="meta">${t('markdown.unrenderable')}</p>${code(block.language || 'markdown', block.text)}</section>`;
    }
  };
  const documentView = document => `<article class="document" id="document-${id(document.path)}"><p class="document-identity"><code>${e(document.path)}</code></p>${(document.blocks || []).map(markdownBlock).join('')}</article>`;
  const visibility = value => value === 'HIDDEN' ? `<span class="badge HIDDEN">${t('visibility.hidden')}</span>` : `<span class="badge PUBLIC">${t('visibility.public')}</span>`;
  const method = item => item?.sourceCode ? `<details><summary>${t('method.source')}</summary><p class="meta">${t('method.description')}</p>${item.methodIdentity ? `<p class="technical-meta"><code>${e(item.methodIdentity)}</code>${item.sourceFile ? `, ${e(item.sourceFile)}:${e(item.sourceLine)}` : ''}</p>` : ''}${code('java', item.sourceCode)}</details>` : `<p class="meta">${t('method.unavailable')}</p>`;
  const reviewCase = item => `<article class="case-card"><p>${visibility(item.visibility)} <strong>${e(item.caseId)}</strong></p>${scenario(item.scenario?.length ? item.scenario : [], false)}<details class="case-values"><summary>${t('case.valuesSummary')}</summary><div class="case-grid"><section><h4>${t('inputs')}</h4>${values(item.inputs)}</section><section><h4>${t('expectedResult')}</h4>${values(item.expected)}</section></div></details></article>`;
  const reviewProperties = properties => !(properties || []).length ? '' : `<section><h4>${t('property.declarations')}</h4>${properties.map(property => `<article class="case-card"><strong>${e(property.title)}</strong><p class="meta"><code>${e(property.methodIdentity)}</code>. ${t('property.tries', e(property.tries), e(property.maxDiscards), e(property.maxShrinks))}</p><details><summary>${t('property.details')}</summary><p class="technical-meta">${e(property.sourceFile)}:${e(property.sourceLine)}</p>${code('java', property.sourceCode)}</details></article>`).join('')}</section>`;
  const advisories = () => !(data.contractQualityAdvisories || []).length ? '' : `<section class="report-section" id="contract-quality-advisories"><h2>${t('advisory.heading')}</h2><p>${t('advisory.description')}</p>${data.contractQualityAdvisories.map(advisory => `<div class="advisory"><p><strong>${e(advisory.ruleCode)}</strong> ${t('advisory.for')} <a href="#review-${id(advisory.acId)}">${e(advisory.acId)}</a></p><p>${e(advisory.expectedPath)}. ${t('advisory.publicRows', e(advisory.publicCount))} ${t('advisory.reviewerRows', e(advisory.hiddenCount))}</p></div>`).join('')}</section>`;
  const reviewAc = (item, inlineProjection = false) => `<article class="ac-review topplecat-projection" id="review-${id(item.acId)}"><div class="projection-label"><strong>${t('review.projection')}</strong><span>${t('review.projectionDescription')}</span></div><div class="ac-heading"><span class="ac-id">${e(item.acId)}</span><h3>${e(item.title)}</h3></div>${item.location?.documentPath ? `<p class="meta">${t('review.selectedSdd')}: <code>${e(item.location.documentPath)}</code>, ${t('review.documentPosition', e(item.location.documentPosition))}</p>` : `<p class="meta">${t('review.noSpec')}</p>`}<h4>${t('review.material')}</h4>${(item.cases || []).map(reviewCase).join('') || `<p class="meta">${t('review.noRows')}</p>`}${reviewProperties(item.properties)}${method(item.method)}<details><summary>${t('review.techSummary')}</summary><p class="technical-meta">${t('review.techMeta')}</p></details></article>`;
  const reviewPage = () => {
    document.title = t('review.title'); document.getElementById('title').textContent = t('review.title'); document.getElementById('notice').textContent = t('review.notice');
    const docs = data.selectedSpecDocuments || [];
    const docIntro = docs.length ? t('review.withDocs') : t('review.withoutDocs');
    document.getElementById('summary').innerHTML = `<section class="report-intro"><h2>${t('review.notice')}</h2><p>${docIntro}</p></section>`;
    const documentHtml = docs.length ? `<section class="report-section" id="selected-documents"><h2>${t('review.selectedDocuments')}</h2>${docs.map(documentView).join('')}</section>` : `<section class="report-section" id="executable-material"><h2>${t('review.material')}</h2>${(data.acceptanceConditions || []).map(item => reviewAc(item, false)).join('')}</section>`;
    document.getElementById('report').innerHTML = `${documentHtml}${advisories()}`;
    document.getElementById('outline').innerHTML = `<h2>${t('review.outline')}</h2>${docs.map(doc => `<a href="#document-${id(doc.path)}">${e(doc.path)}</a>`).join('')}${(data.acceptanceConditions || []).map(ac => `<a href="#review-${id(ac.acId)}">${e(ac.acId)}</a>`).join('')}${(data.contractQualityAdvisories || []).length ? `<a href="#contract-quality-advisories">${t('review.advisories')}</a>` : ''}`;
  };
  const gate = name => (data.gates || []).find(item => item.name === name) || { name, verdict: 'INCOMPLETE', reason: t('gate.evidenceUnavailable') };
  const gateCard = item => `<div class="gate-card ${e(item.verdict)}"><p>${badge(item.verdict)} <strong>${e(item.name)}</strong></p>${item.reason ? `<p>${t('gate.canonicalReason', `<code>${e(item.reason)}</code>`)}</p>` : ''}</div>`;
  const failedSteps = item => (item.steps || []).filter(step => step.status === 'FAIL');
  const differenceLabel = kind => t(`comparison.kind.${kind}`);
  const comparison = item => {
    const step = failedSteps(item).find(candidate => (candidate.comparisons || []).length) || (item.steps || []).find(candidate => (candidate.comparisons || []).length);
    const comparisons = step?.comparisons || [];
    if (!comparisons.length) return '';
    return `<section class="comparison"><h4>${t('comparison.heading')}</h4><p class="comparison-scope">${t('comparison.scope')}</p>${comparisons.map(entry => `<p class="comparison-key">${t('comparison.expected')} <code>${e(entry.expectedKey)}</code></p><div class="table-wrap"><table><thead><tr><th>${t('comparison.path')}</th><th>${t('comparison.expected')}</th><th>${t('comparison.actual')}</th><th>${t('comparison.difference')}</th></tr></thead><tbody>${(entry.differences || []).map(diff => `<tr><td><code>${e(diff.path)}</code></td><td class="expected-value">${e(JSON.stringify(diff.expected))}</td><td class="actual-value">${e(JSON.stringify(diff.actual))}</td><td>${e(differenceLabel(diff.kind))}</td></tr>`).join('')}</tbody></table></div>`).join('')}<p class="technical-meta">${t('comparison.boundTo')} <code>${e(step.stepId)}</code>.</p></section>`;
  };
  const stepData = item => {
    const recorded = (item.steps || []).filter(step => (step.actualArguments || []).length);
    if (!recorded.length) return '';
    return `<section class="step-data"><h4>${t('stepData.heading')}</h4>${recorded.map(step => `<details><summary>${t('stepData.arguments')} <code>${e(step.stepId)}</code></summary>${values(step.actualArguments)}</details>`).join('')}</section>`;
  };
  const lazyCases = new Map();
  const verificationCaseContent = (ac, item) => `<p class="case-contract"><strong>${t('case.publicRule')}:</strong> <a href="#verification-${id(ac.acId)}">${e(ac.acId)}: ${e(ac.title)}</a></p><section class="case-input"><h4>${t('inputs')}</h4>${values(item.inputs)}</section>${comparison(item)}<details class="complete-expected" id="complete-expected-${id(item.caseId)}"><summary>${t('case.completeExpected')}</summary>${values(item.expected)}</details><details class="execution-details" id="execution-${id(item.caseId)}"><summary>${t('case.executionDetails')}</summary><h4>${t('case.scenario')}</h4>${scenario(item.steps?.length ? item.steps.map(step => ({ ...step, phase: ac.stepPhases?.[step.stepId] || 'AND' })) : ac.scenario, Boolean(item.steps?.length))}<h4>${t('case.failedStep')}</h4>${(() => { const last = failedSteps(item)[0] || (item.steps || []).filter(step => step.status !== 'SKIPPED').at(-1); return last ? `<p><code>${e(last.stepId)}</code> ${e(last.sentence)}</p>` : `<p class="meta">${t('case.noStep')}</p>`; })()}${stepData(item)}</details><details class="raw-failure" id="raw-failure-${id(item.caseId)}"><summary>${t('case.rawFailure')}</summary>${item.failure ? `<pre>${e(item.failure)}</pre>` : `<p class="meta">${t('case.noRawFailure')}</p>`}<h5>${t('case.expectedConsumption')}</h5>${values(item.expectedConsumption || {})}</details>`;
  const verificationCase = (ac, item) => {
    const key = JSON.stringify([ac.acId, item.caseId]);
    lazyCases.set(key, { ac, item });
    return `<details class="case-card" data-case-id="${e(item.caseId)}" data-case-status="${e(item.status)}" data-search="${e(`${ac.acId} ${ac.title} ${item.caseId}`.toLowerCase())}" data-lazy-case="${e(key)}" id="case-${id(item.caseId)}"><summary>${visibility(item.visibility)} <strong>${e(item.caseId)}</strong> ${badge(item.status)}</summary><div class="lazy-case-content"></div></details>`;
  };
  const integrityFailed = () => gate('CONTRACT_INTEGRITY').verdict !== 'PASS';
  const contractIntegritySummary = () => {
    const verdict = gate('CONTRACT_INTEGRITY').verdict;
    return verdict === 'PASS'
      ? t('verification.contractMatchesSeal')
      : verdict === 'FAIL'
        ? t('verification.contractMismatch')
        : t('verification.contractUnverified');
  };
  const propertyResults = () => (data.acceptanceConditions || []).flatMap(ac => (ac.properties || []).map(property => `<article class="case-card" id="property-${id(property.methodIdentity)}"><h3>${e(property.title)} ${badge(property.status)}</h3><p class="meta"><code>${e(property.methodIdentity)}</code>. ${t('property.results', e(property.completedTrials), e(property.requestedTrials), e(property.discards))}</p>${property.incompleteReason ? `<p>${e(property.incompleteReason)}</p>` : ''}${property.originalCounterexample ? `<h4>${t('property.original')}</h4>${code('json', property.originalCounterexample.choicesJson)}` : ''}${property.shrunkCounterexample ? `<h4>${t('property.shrunk')}</h4>${code('json', property.shrunkCounterexample.choicesJson)}` : ''}${property.replayToken ? `<p class="meta">${t('property.replay')} <code>${e(property.replayToken)}</code></p>` : ''}</article>`)).join('') || `<p class="meta">${t('property.none')}</p>`;
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
  const safeguardFor = (ac, name) => (ac.safeguards || []).find(item => item.name === name) || { name, verdict: 'INCOMPLETE', outcome: 'UNABLE_TO_ASSESS', reason: 'GATE_RECORDED', explanation: t('gate.evidenceUnavailable'), technicalGate: '' };
  const safeguardLabel = safeguard => safeguard.outcome === 'COMPARISON_COMPLETED' ? t('acResults.comparisonCompleted') : safeguard.outcome === 'PROBLEM_FOUND' ? t('acResults.failed') : safeguard.outcome === 'PASSED' ? t('acResults.passed') : safeguard.outcome === 'DISABLED' ? t('acResults.disabled') : safeguard.outcome === 'NOT_APPLICABLE' ? t('acResults.notApplicable') : t('acResults.incomplete');
  const safeguardReason = safeguard => t(`safeguard.reason.${safeguard.reason}`);
  const safeguardExplanation = safeguard => `<p class="safeguard-explanation">${e(safeguardReason(safeguard))}</p>${safeguard.reason === 'GATE_RECORDED' && safeguard.explanation ? `<p class="meta">${t('acResults.recordedReason', `<code>${e(safeguard.explanation)}</code>`)}</p>` : ''}`;
  const safeguardCard = (ac, name, title, body = '') => {
    const safeguard = safeguardFor(ac, name);
    const heading = name === 'MUTATION_TESTING' ? informationControl('mutationTesting') : e(title);
    return `<section class="ac-safeguard ${e(safeguard.verdict)} outcome-${e(safeguard.outcome)}" id="ac-${id(ac.acId)}-${id(name.toLowerCase())}"><div class="safeguard-heading"><h4>${heading}</h4>${statusBadge(safeguard.verdict, safeguardLabel(safeguard))}</div>${safeguardExplanation(safeguard)}${body}</section>`;
  };
  const casesFor = (ac, visibilityName) => (ac.cases || []).filter(item => item.visibility === visibilityName);
  const casesMarkup = (ac, visibilityName) => {
    const rows = casesFor(ac, visibilityName);
    return rows.length ? rows.map(item => verificationCase(ac, item)).join('') : `<p class="meta">${t('case.noCurrentRows')}</p>`;
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
      const failed = property.status === 'FAIL' ? `<p>${t('property.failed')}</p>` : '';
      return `<article class="case-card property-result" id="property-${id(property.methodIdentity)}"><h3>${e(property.title)} ${badge(property.status)}</h3><p class="meta">${t('property.results', e(property.completedTrials), e(property.requestedTrials), e(property.discards))}</p>${failed}${incomplete}${original}${shrunk}${replay}${discardedInputs(property)}</article>`;
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
      item.methodDescription ? `${informationControl('descriptor')}: <code>${e(item.methodDescription)}</code>` : '',
      item.lineNumber != null ? t('mutation.line', `<code>${e(item.lineNumber)}</code>`) : ''
    ].filter(Boolean).map(line => `<li>${line}</li>`).join('');
    const source = item.originalSourceLine
      ? `<h6>${informationControl('originalSourceLine')}</h6>${code('java', item.originalSourceLine)}`
      : '';
    return `<article class="undetected-mutation" data-mutation-ordinal="${e(item.ordinal)}"><h5>${t('mutation.cardTitle', informationControl('undetectedMutation'), e(item.ordinal))}</h5><h6>${t('mutation.whatChanged')}</h6>${changed}<h6>${t('mutation.whereChanged')}</h6>${location ? `<ul class="mutation-location">${location}</ul>` : `<p class="meta">${t('mutation.noLocation')}</p>`}${source}<h6>${t('mutation.whatHappened')}</h6><p>${t('mutation.acPassed')}</p></article>`;
  };
  const mutationBody = ac => {
    const safeguard = safeguardFor(ac, 'MUTATION_TESTING');
    const assessment = mutationFor(ac);
    if (safeguard.verdict === 'DISABLED' || safeguard.verdict === 'INCOMPLETE') return '';
    if (!assessment) {
      return `<p>${e(safeguard.verdict === 'DISABLED' || safeguard.verdict === 'INCOMPLETE' ? safeguard.explanation : t('mutation.noAttributed'))}</p>`;
    }
    if (assessment.attributionGap) return `<p>${t('mutation.noAttributed')}</p>`;
    const undetected = ac.undetectedMutations || [];
    return `<p>${t('mutation.summary', e(assessment.coveredMutantCount), informationControl('attributedChanges'), e(assessment.killedByAcceptanceMethodMutantCount), e(undetected.length))}</p>${undetected.map(mutationDetail).join('')}`;
  };
  const safeguardOverviewItem = (ac, item) => {
    const label = t(`acResults.${item.name === 'PUBLIC_ACCEPTANCE' ? 'publicAcceptance' : item.name === 'HIDDEN_TESTS' ? 'hiddenTests' : item.name === 'EXPECTED_RESULT_CHECK' ? 'expectedResult' : item.name === 'PROPERTY_BASED_TESTING' ? 'propertyTesting' : 'mutationTesting'}`);
    const outcome = safeguardLabel(item);
    const reason = safeguardReason(item);
    const needsAttention = item.verdict === 'FAIL' || item.verdict === 'INCOMPLETE';
    const attentionClass = needsAttention ? ' requires-attention' : '';
    const reasonMarkup = needsAttention ? `<span class="safeguard-chip-reason">${e(reason)}</span>` : '';
    return `<a class="safeguard-chip ${e(item.verdict)} outcome-${e(item.outcome)}${attentionClass}" href="#ac-${id(ac.acId)}-${id(item.name.toLowerCase())}" aria-label="${e(`${label}: ${outcome}. ${reason}`)}"><span class="safeguard-chip-label">${e(label)}</span><strong>${e(outcome)}</strong>${reasonMarkup}</a>`;
  };
  const acCard = ac => {
    const statusText = ac.status === 'FAIL' ? t('verification.failedAc') : ac.status === 'NOT_REPORTED' ? t('verification.incompleteAc') : t('verification.passedAc');
    const overview = (ac.safeguards || []).map(item => safeguardOverviewItem(ac, item)).join('');
    const acStatusLabel = ac.status === 'NOT_REPORTED' ? t('acResults.incomplete') : ac.status === 'FAIL' ? t('acResults.failed') : t('acResults.passed');
    const identityId = `ac-identity-${id(ac.acId)}`;
    const readerId = `ac-reader-${id(ac.acId)}`;
    const readerLabel = t('verification.readerDetails', `${ac.acId}: ${ac.title}`);
    const readerDetails = `<div class="ac-reader" id="${readerId}" role="region" aria-labelledby="${identityId}" aria-label="${e(readerLabel)}" hidden><div class="ac-card-body">${safeguardCard(ac, 'PUBLIC_ACCEPTANCE', t('acResults.publicAcceptance'), `<h5>${t('acResults.publicCases')}</h5>${casesMarkup(ac, 'PUBLIC')}`)}${safeguardCard(ac, 'HIDDEN_TESTS', t('acResults.hiddenTests'), `<h5>${t('acResults.hiddenCases')}</h5>${casesMarkup(ac, 'HIDDEN')}`)}${safeguardCard(ac, 'EXPECTED_RESULT_CHECK', t('acResults.expectedResult'))}${safeguardCard(ac, 'PROPERTY_BASED_TESTING', t('acResults.propertyTesting'), propertyResultsFor(ac))}${safeguardCard(ac, 'MUTATION_TESTING', t('acResults.mutationTesting'), `<p class="mutation-intro">${t('mutation.intro')}</p>${mutationBody(ac)}`)}<details class="ac-technical" id="ac-technical-${id(ac.acId)}"><summary>${t('technical.heading')}</summary><p class="meta">${t('technical.description')}</p>${(ac.safeguards || []).map(item => `<p><code>${e(item.technicalGate)}</code>: ${e(item.explanation)}</p>`).join('')}</details></div></div>`;
    return `<article class="ac-card ${e(ac.status)}" id="verification-${id(ac.acId)}" data-ac-id="${e(ac.acId)}" data-expanded="false"><div class="ac-identity-row" data-ac-identity><div class="ac-identity" id="${identityId}"><span class="ac-id">${e(ac.acId)}</span><h3>${e(ac.title)}</h3>${statusBadge(ac.status, acStatusLabel)}</div><button class="ac-reading-control" type="button" data-ac-toggle="${e(ac.acId)}" aria-controls="${readerId}" aria-expanded="false" aria-label="${e(t('verification.expandThis'))} ${e(ac.acId)}">${t('verification.expandThis')}</button></div><div class="ac-key-result"><p class="ac-result ${e(ac.status)}"><strong>${t('verification.acResult')}:</strong> ${e(statusText)}</p><nav class="safeguard-overview" aria-label="${e(t('verification.acResult'))}">${overview}</nav></div>${readerDetails}</article>`;
  };
  const needsAttention = acs => {
    const items = acs.filter(ac => ac.status !== 'PASS');
    return `<section class="problem-summary" id="problems"><h2>${t('problems.heading')}</h2>${items.length ? `<ol>${items.map(ac => `<li><a href="#verification-${id(ac.acId)}">${e(ac.acId)}: ${e(ac.title)}</a>. ${e(ac.status === 'FAIL' ? t('verification.failedAc') : t('verification.incompleteAc'))}</li>`).join('')}</ol>` : `<p>${t('problems.noEvidence')}</p>`}</section>`;
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
      const attributed = (item.attributedAcceptanceConditionIds || []).map(ac => `<li><code>${e(ac)}</code></li>`).join('') || `<li>${t('common.none')}</li>`;
      const detected = (item.detectedAcceptanceConditionIds || []).map(ac => `<li><code>${e(ac)}</code></li>`).join('') || `<li>${t('common.none')}</li>`;
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
    const run = data.run || {}; const scope = data.deliveryScope || {}; const selectedIds = scope.acceptanceConditionIds || []; const selected = selectedIds.join(', ') || t('verification.fullContract'); const scopeSource = !selectedIds.length ? t('verification.fullContract') : (scope.specDocuments || []).length ? t('verification.scopeFromSpec') : t('verification.scopeFromAc'); const scopedPassExplanation = data.verdict === 'PASS' && selectedIds.length ? `<p class="meta scoped-pass">${e(t('verification.scopedPass'))}</p>` : '';
    const acs = data.acceptanceConditions || [];
    const passed = run.passedAcceptanceConditionCount ?? acs.filter(ac => ac.status === 'PASS').length;
    const failed = run.failedAcceptanceConditionCount ?? acs.filter(ac => ac.status === 'FAIL').length;
    const incomplete = run.incompleteAcceptanceConditionCount ?? acs.filter(ac => ac.status === 'NOT_REPORTED').length;
    const integrityVerdict = gate('CONTRACT_INTEGRITY').verdict;
    document.getElementById('summary').innerHTML = `<section class="report-intro verification ${e(data.verdict)}"><h2>${e(conclusion)}</h2><p>${t('verification.aggregate', e(acs.length), e(passed), e(failed), e(incomplete))}</p><section class="contract-integrity-summary ${e(integrityVerdict)}" data-integrity-verdict="${e(integrityVerdict)}" aria-label="${e(t('verification.contractIntegrity'))}"><span>${e(t('verification.contractIntegrity'))}</span><strong>${e(contractIntegritySummary())}</strong></section><p class="meta">${t('verification.run', `<code>${e(run.runId || t('verification.unavailable'))}</code>`, e(run.startedAt || t('verification.unavailable')), e(run.finishedAt || data.generatedAt || t('verification.unavailable')))}</p><p class="meta">${t('verification.scope', e(scopeSource), e(selected), e(scope.executedHiddenRows ?? 0), e(scope.executedPublicProperties ?? 0))}</p>${scopedPassExplanation}</section><section class="filter-controls" aria-label="${t('verification.filters')}"><label>${t('verification.find')} <input id="case-query" type="search" autocomplete="off"></label>${['FAIL','PASS','NOT_REPORTED'].map(status => `<button type="button" data-status-filter="${status}" aria-pressed="false">${status}</button>`).join('')}</section>${needsAttention(acs)}`;
    const blocked = `<p class="suppressed">${contractIntegritySummary()}</p>`;
    const readingToolbar = `<div class="ac-reading-toolbar" data-ac-toolbar role="toolbar" aria-label="${e(t('verification.readingToolbar'))}" aria-controls="ac-list" aria-busy="false"><button type="button" class="global-reading-control" data-global-reading aria-expanded="false">${t('verification.expandAll')}</button><span class="bulk-reading-status" data-bulk-status data-completed="0" data-total="0" role="status" aria-live="polite"></span></div>`;
    document.getElementById('report').innerHTML = `<section class="report-section verification-workspace" id="all-acs"><h2>${t('verification.allAcs')}</h2>${integrityFailed() ? blocked : `${readingToolbar}<div id="ac-list" class="ac-list">${acs.map(acCard).join('')}</div>`}</section>${technicalEvidence()}`;
    document.getElementById('outline').innerHTML = `<h2>${t('verification.outline')}</h2><a href="#problems">${t('verification.needsAttention')}</a><a href="#all-acs">${t('verification.allAcs')}</a><a href="#technical-evidence">${t('technical.heading')}</a>`;
  };
  const acCards = () => [...document.querySelectorAll('#ac-list > .ac-card')];
  const cardForAcId = acId => acCards().find(card => card.dataset.acId === acId);
  let activeAcId = null;
  let bulkOperation = null;
  let fragmentScrollToken = 0;
  const setActiveAc = card => {
    if (card?.dataset.acId) {
      activeAcId = card.dataset.acId;
      updateStickyOffset();
    }
  };
  const activeAcCard = () => cardForAcId(activeAcId) || acCards()[0] || null;
  const captureAnchor = card => card ? { card, top: card.getBoundingClientRect().top } : null;
  const restoreAnchor = anchor => {
    if (!anchor?.card || typeof window.scrollBy !== 'function') return;
    const delta = anchor.card.getBoundingClientRect().top - anchor.top;
    if (Math.abs(delta) > 0.5) window.scrollBy(0, delta);
  };
  const focusWithoutScroll = element => {
    if (!element || typeof element.focus !== 'function') return;
    try { element.focus({ preventScroll: true }); } catch (_error) { element.focus(); }
  };
  const materializeCase = details => {
    if (!details || details.dataset.loaded === 'true') return;
    const record = lazyCases.get(details.dataset.lazyCase);
    if (!record) return;
    details.querySelector('.lazy-case-content').innerHTML = verificationCaseContent(record.ac, record.item);
    details.dataset.loaded = 'true';
  };
  const setCaseExpanded = (details, expanded) => {
    if (expanded) materializeCase(details);
    details.open = expanded;
  };
  const setAcExpanded = (card, expanded) => {
    if (!card) return;
    const reader = card.querySelector('.ac-reader');
    const control = card.querySelector('[data-ac-toggle]');
    if (!reader || !control) return;
    reader.hidden = !expanded;
    card.dataset.expanded = String(expanded);
    control.setAttribute('aria-expanded', String(expanded));
    const label = expanded ? t('verification.collapseThis') : t('verification.expandThis');
    control.textContent = label;
    control.setAttribute('aria-label', `${label} ${card.dataset.acId}`);
    reader.querySelectorAll('details[data-lazy-case]').forEach(details => setCaseExpanded(details, expanded));
  };
  const globalReadingControl = () => document.querySelector('[data-global-reading]');
  const setGlobalState = (state, status = '', completed = 0) => {
    const toolbar = document.querySelector('[data-ac-toolbar]');
    const control = globalReadingControl();
    const live = document.querySelector('[data-bulk-status]');
    const total = acCards().length;
    if (!toolbar || !control || !live) return;
    toolbar.setAttribute('aria-busy', String(state === 'busy'));
    control.disabled = total === 0;
    live.dataset.completed = String(completed);
    live.dataset.total = String(total);
    control.setAttribute('aria-expanded', String(state === 'expanded'));
    control.textContent = state === 'busy'
      ? t('verification.stopBulk')
      : state === 'expanded' ? t('verification.collapseAll') : t('verification.expandAll');
    live.textContent = status;
  };
  const collapseAllAcReaders = (status = t('verification.keyResultsOnly'), completed = 0) => {
    const anchor = captureAnchor(activeAcCard());
    acCards().forEach(card => setAcExpanded(card, false));
    setGlobalState('key', status, completed);
    restoreAnchor(anchor);
    focusWithoutScroll(globalReadingControl());
  };
  const scheduleBulkBatch = (token, callback) => {
    token.timer = window.setTimeout(callback, 16);
  };
  const stopBulkExpansion = () => {
    if (!bulkOperation) return false;
    const token = bulkOperation;
    token.cancelled = true;
    if (token.timer != null) window.clearTimeout(token.timer);
    bulkOperation = null;
    collapseAllAcReaders(t('verification.expansionInterrupted', token.completed, token.total), token.completed);
    return true;
  };
  const expandAllAcReaders = () => {
    if (bulkOperation) return stopBulkExpansion();
    const cards = acCards();
    if (!cards.length) return false;
    const anchor = captureAnchor(activeAcCard());
    const token = { cancelled: false, completed: 0, total: cards.length, timer: null, anchor, anchorIndex: anchor ? cards.indexOf(anchor.card) : -1 };
    bulkOperation = token;
    cards.forEach(card => setAcExpanded(card, false));
    if (token.anchorIndex > 0) restoreAnchor(token.anchor);
    setGlobalState('busy', t('verification.expanding', 0, token.total), 0);
    const batchSize = 4;
    const runBatch = () => {
      if (bulkOperation !== token || token.cancelled) return;
      try {
        const end = Math.min(token.completed + batchSize, token.total);
        for (; token.completed < end; token.completed += 1) setAcExpanded(cards[token.completed], true);
        if (token.anchorIndex > 0) restoreAnchor(token.anchor);
        if (token.completed >= token.total) {
          bulkOperation = null;
          setGlobalState('expanded', t('verification.expandedAll'), token.total);
          if (token.anchorIndex > 0) restoreAnchor(token.anchor);
          focusWithoutScroll(globalReadingControl());
          return;
        }
        setGlobalState('busy', t('verification.expanding', token.completed, token.total), token.completed);
        scheduleBulkBatch(token, runBatch);
      } catch (_error) {
        bulkOperation = null;
        setGlobalState('key', t('verification.expansionInterrupted', token.completed, token.total), token.completed);
        if (token.anchorIndex > 0) restoreAnchor(token.anchor);
        focusWithoutScroll(globalReadingControl());
      }
    };
    scheduleBulkBatch(token, runBatch);
    return true;
  };
  const revealDisclosurePath = target => {
    let disclosure = target?.closest?.('details') || null;
    while (disclosure) {
      if (disclosure.dataset.lazyCase) materializeCase(disclosure);
      disclosure.open = true;
      disclosure = disclosure.parentElement?.closest?.('details') || null;
    }
  };
  const resolveHashTarget = hash => {
    const raw = String(hash || '').replace(/^#/, '');
    if (!raw) return null;
    let target = document.getElementById(raw);
    if (target) return target;
    try { target = document.getElementById(decodeURIComponent(raw)); } catch (_error) { target = null; }
    if (target) return target;
    for (const record of lazyCases.values()) {
      if (![`complete-expected-${id(record.item.caseId)}`, `execution-${id(record.item.caseId)}`, `raw-failure-${id(record.item.caseId)}`].includes(raw)) continue;
      const details = document.getElementById(`case-${id(record.item.caseId)}`);
      if (details) materializeCase(details);
      target = document.getElementById(raw);
      if (target) return target;
    }
    return null;
  };
  const stickyTargetGap = () => {
    const value = typeof window.getComputedStyle === 'function'
      ? window.getComputedStyle(document.documentElement).getPropertyValue('--ac-reading-target-gap')
      : '';
    const match = String(value).trim().match(/^([0-9.]+)(px|rem|em)$/);
    if (!match) return 0;
    const amount = Number.parseFloat(match[1]);
    if (!Number.isFinite(amount)) return 0;
    if (match[2] === 'px') return amount;
    const rootFontSize = Number.parseFloat(window.getComputedStyle(document.documentElement).fontSize);
    return Number.isFinite(rootFontSize) ? amount * rootFontSize : 0;
  };
  const scrollTargetIntoView = target => {
    if (!target || typeof target.scrollIntoView !== 'function') return;
    const token = ++fragmentScrollToken;
    let frameCount = 0;
    let stableFrames = 0;
    let previousStickyBottom = null;
    const schedule = callback => {
      if (typeof window.requestAnimationFrame === 'function') window.requestAnimationFrame(callback);
      else window.setTimeout(callback, 0);
    };
    const align = initial => {
      if (token !== fragmentScrollToken) return;
      updateStickyOffset();
      const runInstantScroll = callback => {
        const previousBehavior = document.documentElement.style.scrollBehavior;
        document.documentElement.style.scrollBehavior = 'auto';
        try { callback(); } finally { document.documentElement.style.scrollBehavior = previousBehavior; }
      };
      if (initial) runInstantScroll(() => target.scrollIntoView({ behavior: 'auto', block: 'start', inline: 'nearest' }));
      const toolbarBottom = document.querySelector('[data-ac-toolbar]')?.getBoundingClientRect().bottom || 0;
      const identityBottom = target.closest?.('.ac-card')?.querySelector('[data-ac-identity]')?.getBoundingClientRect().bottom || 0;
      const stickyBottom = Math.max(toolbarBottom, identityBottom);
      const desiredTop = stickyBottom + stickyTargetGap();
      const targetTop = target.getBoundingClientRect().top;
      const delta = targetTop - desiredTop;
      if (Math.abs(delta) > 0.5) {
        runInstantScroll(() => {
          if (typeof window.scrollBy === 'function') window.scrollBy(0, delta);
          else if (typeof window.scrollTo === 'function') window.scrollTo(window.scrollX, window.scrollY + delta);
        });
      }
      stableFrames = previousStickyBottom != null && Math.abs(stickyBottom - previousStickyBottom) < 0.5
        ? stableFrames + 1 : 0;
      previousStickyBottom = stickyBottom;
      frameCount += 1;
      if (frameCount < 16 && (stableFrames < 3 || targetTop < desiredTop - 0.5)) schedule(() => align(false));
    };
    schedule(() => align(true));
  };
  const revealHash = (hash, scroll = true) => {
    const target = resolveHashTarget(hash);
    if (!target) return false;
    const card = target.closest?.('.ac-card');
    if (card) {
      setActiveAc(card);
      if (card.dataset.expanded !== 'true') setAcExpanded(card, true);
    }
    revealDisclosurePath(target);
    updateStickyOffset();
    if (scroll) scrollTargetIntoView(target);
    return true;
  };
  const handleHashLink = event => {
    const link = event.target?.closest?.('a[href^="#"]');
    if (!link) return;
    const hash = link.getAttribute('href');
    if (!hash || hash === '#' || !resolveHashTarget(hash)) return;
    event.preventDefault();
    revealHash(hash, false);
    if (window.history?.pushState) window.history.pushState(null, '', hash); else window.location.hash = hash;
    const target = resolveHashTarget(hash);
    if (target) scrollTargetIntoView(target);
  };
  const updateStickyOffset = () => {
    const toolbar = document.querySelector('[data-ac-toolbar]');
    const rootStyle = document.documentElement.style;
    rootStyle.setProperty('--global-ac-toolbar-height', `${toolbar?.getBoundingClientRect().height || 0}px`);
    const identity = activeAcCard()?.querySelector('[data-ac-identity]');
    rootStyle.setProperty('--active-ac-identity-height', `${identity?.getBoundingClientRect().height || 0}px`);
  };
  let activeInformation = null;
  const informationParts = wrapper => ({
    button: wrapper?.querySelector('[data-info-button]'),
    popover: wrapper?.querySelector('[data-info-popover]')
  });
  const positionInformation = wrapper => {
    const { popover } = informationParts(wrapper);
    if (!popover || popover.hidden) return;
    const wrapperRect = wrapper.getBoundingClientRect();
    const viewportWidth = window.innerWidth || document.documentElement.clientWidth || 1024;
    const gap = 12;
    const popoverWidth = popover.getBoundingClientRect().width || Math.min(480, viewportWidth - gap * 2);
    const rightSafeOffset = viewportWidth - gap - wrapperRect.left - popoverWidth;
    const leftSafeOffset = gap - wrapperRect.left;
    const left = Math.min(Math.max(0, leftSafeOffset), rightSafeOffset);
    popover.style.left = `${left}px`;
  };
  const closeInformation = wrapper => {
    const { button, popover } = informationParts(wrapper);
    if (!button || !popover) return;
    popover.hidden = true;
    popover.style.left = '';
    button.setAttribute('aria-expanded', 'false');
    delete wrapper.dataset.infoOpen;
    delete wrapper.dataset.infoPinned;
    if (activeInformation === wrapper) activeInformation = null;
  };
  const openInformation = (button, pinned = false) => {
    const wrapper = button?.closest?.('[data-info-wrapper]');
    if (!wrapper) return;
    if (activeInformation && activeInformation !== wrapper) closeInformation(activeInformation);
    const { popover } = informationParts(wrapper);
    if (!popover) return;
    popover.hidden = false;
    wrapper.dataset.infoOpen = 'true';
    if (pinned) wrapper.dataset.infoPinned = 'true';
    button.setAttribute('aria-expanded', 'true');
    activeInformation = wrapper;
    positionInformation(wrapper);
  };
  const installInformationControls = () => {
    document.querySelectorAll('[data-info-wrapper]').forEach(wrapper => {
      const { button } = informationParts(wrapper);
      if (!button) return;
      const enter = () => {
        wrapper.dataset.infoHovered = 'true';
        openInformation(button);
      };
      const leave = () => {
        wrapper.dataset.infoHovered = 'false';
        if (wrapper.dataset.infoPinned !== 'true' && document.activeElement !== button) {
          closeInformation(wrapper);
        }
      };
      ['mouseenter', 'pointerenter'].forEach(type => wrapper.addEventListener(type, enter));
      ['mouseleave', 'pointerleave'].forEach(type => wrapper.addEventListener(type, leave));
      button.addEventListener('focus', () => openInformation(button));
      button.addEventListener('blur', () => {
        if (wrapper.dataset.infoPinned !== 'true' && wrapper.dataset.infoHovered !== 'true') {
          closeInformation(wrapper);
        }
      });
      button.addEventListener('click', event => {
        event.stopPropagation();
        if (wrapper.dataset.infoPinned === 'true') closeInformation(wrapper);
        else openInformation(button, true);
      });
    });
    document.addEventListener('click', event => {
      if (activeInformation && !activeInformation.contains(event.target)) closeInformation(activeInformation);
    });
    document.addEventListener('keydown', event => {
      if (event.key !== 'Escape' || !activeInformation) return;
      closeInformation(activeInformation);
      event.preventDefault();
    });
    window.addEventListener('resize', () => {
      if (activeInformation) positionInformation(activeInformation);
    });
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
      materializeCase(details);
    }));
    document.querySelectorAll('[data-ac-toggle]').forEach(button => button.addEventListener('click', () => {
      const card = button.closest('.ac-card');
      if (!card) return;
      setActiveAc(card);
      const anchor = captureAnchor(card);
      setAcExpanded(card, button.getAttribute('aria-expanded') !== 'true');
      restoreAnchor(anchor);
      focusWithoutScroll(button);
    }));
    const globalControl = globalReadingControl();
    if (globalControl) globalControl.addEventListener('click', () => {
      if (bulkOperation) {
        stopBulkExpansion();
        return;
      }
      if (acCards().length && acCards().every(card => card.dataset.expanded === 'true')) {
        collapseAllAcReaders();
      } else {
        expandAllAcReaders();
      }
    });
    const updateActiveFromViewport = () => {
      const toolbarHeight = document.querySelector('[data-ac-toolbar]')?.getBoundingClientRect().height || 0;
      const marker = toolbarHeight + 24;
      const candidate = acCards().find(card => {
        const rect = card.getBoundingClientRect();
        return rect.top <= marker && rect.bottom > marker;
      });
      if (candidate) setActiveAc(candidate);
    };
    document.addEventListener('click', handleHashLink);
    window.addEventListener('hashchange', () => revealHash(window.location.hash, true));
    window.addEventListener('scroll', updateActiveFromViewport, { passive: true });
    window.addEventListener('resize', updateStickyOffset);
    updateStickyOffset();
    if (typeof ResizeObserver === 'function') {
      const toolbar = document.querySelector('[data-ac-toolbar]');
      const resizeObserver = new ResizeObserver(updateStickyOffset);
      if (toolbar) resizeObserver.observe(toolbar);
      document.querySelectorAll('[data-ac-identity]').forEach(identity => resizeObserver.observe(identity));
    }
    setGlobalState('key', '');
    if (window.location.hash) revealHash(window.location.hash, true);
    document.querySelectorAll('.discarded-inputs').forEach(container => {
      const items = [...container.querySelectorAll('[data-discard-item]')]; const size = 25; let page = 0;
      const pageCount = Math.max(1, Math.ceil(items.length / size)); const label = container.querySelector('[data-discard-page]');
      const render = () => { items.forEach((item, index) => { item.hidden = Math.floor(index / size) !== page; }); label.textContent = t('property.page', page + 1, pageCount); container.querySelector('[data-discard-prev]').disabled = page === 0; container.querySelector('[data-discard-next]').disabled = page === pageCount - 1; };
      container.querySelector('[data-discard-prev]').addEventListener('click', () => { if (page > 0) { page -= 1; render(); } });
      container.querySelector('[data-discard-next]').addEventListener('click', () => { if (page + 1 < pageCount) { page += 1; render(); } });
      render();
    });
  }
  installInformationControls();
  document.querySelectorAll('.mermaid-diagram').forEach(container => {
    const source = container.querySelector('.mermaid-source')?.textContent || '';
    try { container.innerHTML = window.ToppleCatMermaid.render(source); } catch (_error) { container.innerHTML = `<p class="mermaid-error">${t('mermaid.error')}</p>`; }
  });
})();
