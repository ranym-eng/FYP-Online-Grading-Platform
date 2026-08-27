const presentationIndividual = [
  {
    id: 'present-information',
    label: 'Present technical information to a general audience clearly, logically and in an easy-to-follow manner',
    outcome: '3.c',
    weight: 1,
  },
  {
    id: 'answer-questions',
    label: 'Respond effectively to questions and comments about the project technical solutions',
    outcome: 'Questions and answers',
    weight: 4,
  },
]

const presentationGroup = [
  { id: 'technical-presentation', label: 'Produce a quality technical presentation: slides, technical content and English', outcome: '3.b', weight: 1 },
  { id: 'identify-problem', label: 'Identify and state a complex engineering problem', outcome: '1.a', weight: 1 },
  { id: 'formulate-problem', label: 'Formulate the complex problem using diagrams, equations or flowcharts', outcome: '1.b', weight: 1 },
  { id: 'design-requirements', label: 'Specify design requirements and constraints', outcome: '2.a', weight: 1 },
  { id: 'analyze-solutions', label: 'Analyze and produce solutions: alternatives, simulation or implementation', outcome: '1.c', weight: 2 },
  { id: 'evaluate-ethics-impact', label: 'Evaluate solutions according to ethics and their economic, environmental and societal impacts', outcome: '4.c', weight: 1.5 },
  { id: 'complete-work', label: 'Complete the proposed work', outcome: 'Implementation', weight: 2 },
]

const reportCriteria = [
  { id: 'identify-problem', label: 'Identify and state a complex engineering problem', outcome: '1.a', weight: 1 },
  { id: 'formulate-problem', label: 'Formulate the problem using diagrams, equations or flowcharts', outcome: '1.b', weight: 1 },
  { id: 'design-requirements', label: 'Specify design requirements and constraints', outcome: '2.a', weight: 1 },
  { id: 'analyze-solutions', label: 'Analyze and produce solutions to the complex problem', outcome: '1.c', weight: 2 },
  { id: 'develop-solutions', label: 'Develop and evaluate possible solutions under realistic constraints', outcome: '2.b', weight: 1 },
  { id: 'build-test', label: 'Select components, build and test the solution', outcome: '2.c', weight: 1 },
  { id: 'technical-report', label: 'Write a technical report that follows formatting and language requirements', outcome: '3.a', weight: 1 },
  { id: 'professional-ethics', label: 'Demonstrate command of the code of ethics: citations, similarity and integrity', outcome: '4.a', weight: 1 },
  { id: 'evaluate-impact', label: 'Evaluate global, economic, environmental and societal impacts', outcome: '4.c', weight: 1 },
  { id: 'complete-work', label: 'Complete the proposed work', outcome: 'Implementation', weight: 1 },
]

const supervisorCriteria = [
  { id: 'analyze-solutions', label: 'Analyze and produce solutions to the complex problem', outcome: '1.c', weight: 1 },
  { id: 'build-test', label: 'Select components, build and test the solution', outcome: '2.c', weight: 1 },
  { id: 'professional-responsibility', label: 'Meet professional responsibilities: meetings, punctuality and deadlines', outcome: '4.b', weight: 1 },
  { id: 'plan-objectives', label: 'Define project objectives and prepare an implementation plan', outcome: '5.a', weight: 1 },
  { id: 'assigned-tasks', label: 'Complete assigned tasks to achieve the objectives', outcome: '5.b', weight: 1 },
  { id: 'team-leadership', label: 'Lead the team toward achieving the project objective', outcome: '5.c', weight: 1 },
  { id: 'acquire-information', label: 'Acquire new and relevant information for the project', outcome: '7.a', weight: 1 },
  { id: 'learning-strategies', label: 'Use appropriate research and learning strategies', outcome: '7.b', weight: 1 },
  { id: 'apply-knowledge', label: 'Apply newly acquired knowledge', outcome: '7.c', weight: 1 },
  { id: 'technical-questions', label: 'Answer technical and open questions during meetings and rehearsals', outcome: 'Questions and answers', weight: 1 },
  { id: 'proposal-deadline', label: 'Write and submit the extended proposal on time', outcome: 'Deliverable', weight: 1 },
]

const demoCriteria = [
  { id: 'prototype', label: 'Select components, build and test the project prototype', outcome: '1.c', weight: 2 },
  { id: 'present-prototype', label: 'Present the prototype clearly, logically and in an easy-to-follow manner', outcome: '3.c', weight: 1 },
  { id: 'answer-questions', label: 'Respond effectively to questions and comments', outcome: 'Questions and answers', weight: 4 },
  { id: 'complete-work', label: 'Complete the proposed work', outcome: 'Implementation', weight: 2 },
  { id: 'poster', label: 'Produce a quality poster: design, technical content and English', outcome: 'Poster', weight: 1 },
]

const presentationTemplate = {
  kind: 'presentation',
  label: 'Oral presentation',
  shortFormula: 'Part A × 15/40 + Part B × 25/40',
  sections: [
    { id: 'individual', label: 'Part A · Individual evaluation', target: 'student', criteria: presentationIndividual },
    { id: 'group', label: 'Part B · Project evaluation', target: 'group', criteria: presentationGroup },
  ],
}

const reportTemplate = {
  kind: 'report',
  label: 'Paper report',
  shortFormula: '(C1 + C2 + C3 + 2×C4 + C5…C10) ÷ 11',
  sections: [{ id: 'group', label: 'Project report evaluation', target: 'group', criteria: reportCriteria }],
}

const supervisorTemplate = {
  kind: 'supervisor',
  label: 'Supervisor evaluation',
  shortFormula: 'Sum of the 11 criteria ÷ 11',
  sections: [{ id: 'individual', label: 'Individual monitoring', target: 'student', criteria: supervisorCriteria }],
}

const demoTemplate = {
  kind: 'demo',
  label: 'Demo Day',
  shortFormula: '(2×C1 + C2 + 4×C3 + 2×C4 + C5) ÷ 10',
  sections: [{ id: 'group', label: 'Group evaluation', target: 'group', criteria: demoCriteria }],
}

export const SCORING_TEMPLATES = {
  ORAL_PHASE_I: { ...presentationTemplate, phase: 'FYP I' },
  ORAL_PHASE_II: { ...presentationTemplate, phase: 'FYP II' },
  REPORT_PHASE_I: { ...reportTemplate, phase: 'FYP I' },
  REPORT_PHASE_II: { ...reportTemplate, phase: 'FYP II' },
  SUPERVISOR_PHASE_I: { ...supervisorTemplate, phase: 'FYP I' },
  SUPERVISOR_PHASE_II: { ...supervisorTemplate, phase: 'FYP II' },
  DEMO_DAY_INDUSTRY: { ...demoTemplate, phase: 'FYP II' },
}

export function scoreKey(sectionId, criterionId, targetId) {
  return `${sectionId}:${criterionId}:${targetId}`
}

export function normalizeScore(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return 0
  return Math.min(10, Math.max(0, numeric))
}

export function sectionAverage(section, scores, targetId) {
  const totalWeight = section.criteria.reduce((sum, criterion) => sum + criterion.weight, 0)
  if (!totalWeight) return 0
  const weightedScore = section.criteria.reduce((sum, criterion) => {
    return sum + normalizeScore(scores[scoreKey(section.id, criterion.id, targetId)]) * criterion.weight
  }, 0)
  return weightedScore / totalWeight
}

export function calculateTemplate(template, scores, targetIds) {
  const individual = template.sections.find((section) => section.target === 'student')
  const group = template.sections.find((section) => section.target === 'group')
  const groupScore = group ? sectionAverage(group, scores, 'group') : null
  const ids = individual ? targetIds : ['group']

  return Object.fromEntries(ids.map((targetId) => {
    const individualScore = individual ? sectionAverage(individual, scores, targetId) : null
    const finalScore = template.kind === 'presentation'
      ? individualScore * (15 / 40) + groupScore * (25 / 40)
      : individualScore ?? groupScore ?? 0
    return [targetId, {
      individualScore,
      groupScore,
      finalScore,
      contributionA: template.kind === 'presentation' ? individualScore * (15 / 40) : null,
      contributionB: template.kind === 'presentation' ? groupScore * (25 / 40) : null,
    }]
  }))
}

export function performanceBand(score) {
  if (score >= 8) return { label: 'Very good', tone: 'excellent' }
  if (score >= 6) return { label: 'Good', tone: 'good' }
  if (score >= 5) return { label: 'Satisfactory', tone: 'satisfactory' }
  return { label: 'Needs improvement', tone: 'weak' }
}
