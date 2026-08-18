import assert from 'node:assert/strict'
import {
  SCORING_TEMPLATES,
  calculateTemplate,
  scoreKey,
} from '../src/gradingTemplates.js'

function filledScores(template, studentIds, value) {
  const scores = {}
  for (const section of template.sections) {
    const targets = section.target === 'student' ? studentIds : ['group']
    for (const criterion of section.criteria) {
      for (const target of targets) {
        scores[scoreKey(section.id, criterion.id, target)] = value
      }
    }
  }
  return scores
}

const students = ['s1', 's2']
for (const [type, template] of Object.entries(SCORING_TEMPLATES)) {
  const scores = filledScores(template, students, 10)
  const results = calculateTemplate(template, scores, students)
  for (const result of Object.values(results)) {
    assert.equal(result.finalScore, 10, `${type} doit produire 10/10 avec toutes les notes à 10`)
  }
}

const presentation = SCORING_TEMPLATES.ORAL_PHASE_I
const presentationScores = filledScores(presentation, ['s1'], 0)
presentationScores[scoreKey('individual', 'present-information', 's1')] = 10
presentationScores[scoreKey('individual', 'answer-questions', 's1')] = 5
for (const criterion of presentation.sections[1].criteria) {
  presentationScores[scoreKey('group', criterion.id, 'group')] = 8
}
const presentationResult = calculateTemplate(presentation, presentationScores, ['s1']).s1
assert.equal(presentationResult.individualScore, 6)
assert.equal(presentationResult.groupScore, 8)
assert.equal(presentationResult.finalScore, 7.25)

const industryDemo = SCORING_TEMPLATES.DEMO_DAY_INDUSTRY
assert.deepEqual(industryDemo.sections[0].criteria.map((criterion) => criterion.weight), [2, 1, 4, 2, 1])
const industryScores = {
  [scoreKey('group', 'prototype', 'group')]: 8,
  [scoreKey('group', 'present-prototype', 'group')]: 7,
  [scoreKey('group', 'answer-questions', 'group')]: 9,
  [scoreKey('group', 'complete-work', 'group')]: 6,
  [scoreKey('group', 'poster', 'group')]: 10,
}
const industryResult = calculateTemplate(industryDemo, industryScores, []).group
assert.equal(industryResult.finalScore, 8.1)

const report = SCORING_TEMPLATES.REPORT_PHASE_I
const reportScores = filledScores(report, ['s1'], 5)
reportScores[scoreKey('group', 'analyze-solutions', 'group')] = 10
const reportResult = calculateTemplate(report, reportScores, ['s1']).group
assert.equal(reportResult.finalScore, 65 / 11)

console.log('7 modèles vérifiés; formules de présentation, rapport et Industry Guest vérifiées.')