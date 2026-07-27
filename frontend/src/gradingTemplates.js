const presentationIndividual = [
  {
    id: 'present-information',
    label: 'Présenter les informations techniques à un public général, de manière claire, logique et facile à suivre',
    outcome: '3.c',
    weight: 1,
  },
  {
    id: 'answer-questions',
    label: 'Répondre efficacement aux questions et commentaires liés aux solutions techniques du projet',
    outcome: 'Questions et réponses',
    weight: 4,
  },
]

const presentationGroup = [
  { id: 'technical-presentation', label: 'Produire une présentation technique de qualité: diapositives, contenu technique et anglais', outcome: '3.b', weight: 1 },
  { id: 'identify-problem', label: 'Identifier et énoncer un problème d’ingénierie complexe', outcome: '1.a', weight: 1 },
  { id: 'formulate-problem', label: 'Formuler le problème complexe à l’aide de diagrammes, équations ou organigrammes', outcome: '1.b', weight: 1 },
  { id: 'design-requirements', label: 'Spécifier les exigences et contraintes de conception', outcome: '2.a', weight: 1 },
  { id: 'analyze-solutions', label: 'Analyser et produire des solutions: alternatives, simulation ou réalisation', outcome: '1.c', weight: 2 },
  { id: 'evaluate-ethics-impact', label: 'Évaluer les solutions selon l’éthique et leurs impacts économiques, environnementaux et sociétaux', outcome: '4.c', weight: 1.5 },
  { id: 'complete-work', label: 'Achever le travail proposé', outcome: 'Réalisation', weight: 2 },
]

const reportCriteria = [
  { id: 'identify-problem', label: 'Identifier et énoncer un problème d’ingénierie complexe', outcome: '1.a', weight: 1 },
  { id: 'formulate-problem', label: 'Formuler le problème à l’aide de diagrammes, équations ou organigrammes', outcome: '1.b', weight: 1 },
  { id: 'design-requirements', label: 'Spécifier les exigences et contraintes de conception', outcome: '2.a', weight: 1 },
  { id: 'analyze-solutions', label: 'Analyser et produire des solutions au problème complexe', outcome: '1.c', weight: 2 },
  { id: 'develop-solutions', label: 'Développer et évaluer les solutions possibles sous contraintes réalistes', outcome: '2.b', weight: 1 },
  { id: 'build-test', label: 'Sélectionner les composants, construire et tester la solution', outcome: '2.c', weight: 1 },
  { id: 'technical-report', label: 'Rédiger un rapport technique conforme aux consignes de forme et de langue', outcome: '3.a', weight: 1 },
  { id: 'professional-ethics', label: 'Démontrer la maîtrise du code d’éthique: citations, similarité et intégrité', outcome: '4.a', weight: 1 },
  { id: 'evaluate-impact', label: 'Évaluer les impacts globaux, économiques, environnementaux et sociétaux', outcome: '4.c', weight: 1 },
  { id: 'complete-work', label: 'Achever le travail proposé', outcome: 'Réalisation', weight: 1 },
]

const supervisorCriteria = [
  { id: 'analyze-solutions', label: 'Analyser et produire des solutions au problème complexe', outcome: '1.c', weight: 1 },
  { id: 'build-test', label: 'Sélectionner les composants, construire et tester la solution', outcome: '2.c', weight: 1 },
  { id: 'professional-responsibility', label: 'Respecter les responsabilités professionnelles: réunions, ponctualité et délais', outcome: '4.b', weight: 1 },
  { id: 'plan-objectives', label: 'Définir les objectifs du projet et préparer un plan de réalisation', outcome: '5.a', weight: 1 },
  { id: 'assigned-tasks', label: 'Réaliser les tâches attribuées pour atteindre les objectifs', outcome: '5.b', weight: 1 },
  { id: 'team-leadership', label: 'Conduire l’équipe vers la réalisation de l’objectif du projet', outcome: '5.c', weight: 1 },
  { id: 'acquire-information', label: 'Acquérir des informations nouvelles et pertinentes pour le projet', outcome: '7.a', weight: 1 },
  { id: 'learning-strategies', label: 'Utiliser des stratégies de recherche et d’apprentissage adaptées', outcome: '7.b', weight: 1 },
  { id: 'apply-knowledge', label: 'Appliquer les nouvelles connaissances acquises', outcome: '7.c', weight: 1 },
  { id: 'technical-questions', label: 'Répondre aux questions techniques et ouvertes pendant les réunions et répétitions', outcome: 'Questions et réponses', weight: 1 },
  { id: 'proposal-deadline', label: 'Rédiger et remettre la proposition étendue dans les délais', outcome: 'Livrable', weight: 1 },
]

const demoCriteria = [
  { id: 'prototype', label: 'Sélectionner les composants, construire et tester le prototype du projet', outcome: '1.c', weight: 2 },
  { id: 'present-prototype', label: 'Présenter le prototype de manière claire, logique et facile à suivre', outcome: '3.c', weight: 1 },
  { id: 'answer-questions', label: 'Répondre efficacement aux questions et commentaires', outcome: 'Questions et réponses', weight: 4 },
  { id: 'complete-work', label: 'Achever le travail proposé', outcome: 'Réalisation', weight: 2 },
  { id: 'poster', label: 'Produire une affiche de qualité: conception, contenu technique et anglais', outcome: 'Affiche', weight: 1 },
]

const presentationTemplate = {
  kind: 'presentation',
  label: 'Présentation orale',
  shortFormula: 'Partie A × 15/40 + Partie B × 25/40',
  sections: [
    { id: 'individual', label: 'Partie A · Évaluation individuelle', target: 'student', criteria: presentationIndividual },
    { id: 'group', label: 'Partie B · Évaluation du projet', target: 'group', criteria: presentationGroup },
  ],
}

const reportTemplate = {
  kind: 'report',
  label: 'Rapport technique',
  shortFormula: 'Somme pondérée ÷ 11',
  sections: [{ id: 'individual', label: 'Évaluation du rapport', target: 'student', criteria: reportCriteria }],
}

const supervisorTemplate = {
  kind: 'supervisor',
  label: 'Évaluation du superviseur',
  shortFormula: 'Somme des 11 critères ÷ 11',
  sections: [{ id: 'individual', label: 'Suivi individuel', target: 'student', criteria: supervisorCriteria }],
}

const demoTemplate = {
  kind: 'demo',
  label: 'Demo Day',
  shortFormula: '(2×C1 + C2 + 4×C3 + 2×C4 + C5) ÷ 10',
  sections: [{ id: 'group', label: 'Évaluation du groupe', target: 'group', criteria: demoCriteria }],
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
  if (score >= 8) return { label: 'Très bien', tone: 'excellent' }
  if (score >= 6) return { label: 'Bien', tone: 'good' }
  if (score >= 5) return { label: 'Satisfaisant', tone: 'satisfactory' }
  return { label: 'À améliorer', tone: 'weak' }
}
