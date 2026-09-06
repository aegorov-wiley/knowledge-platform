{{/* Expand the name of the chart. */}}
{{- define "kp.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Fully qualified app name. */}}
{{- define "kp.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "kp.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Common labels. */}}
{{- define "kp.labels" -}}
helm.sh/chart: {{ include "kp.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: knowledge-platform
{{- end -}}

{{/*
Component-scoped selector labels.
Usage: {{ include "kp.selectorLabels" (dict "ctx" . "component" "rag-service") }}
*/}}
{{- define "kp.selectorLabels" -}}
app.kubernetes.io/name: {{ .component }}
app.kubernetes.io/instance: {{ .ctx.Release.Name }}
{{- end -}}

{{- define "kp.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- include "kp.fullname" . -}}
{{- else -}}
default
{{- end -}}
{{- end -}}

{{/* Render a component name prefixed by the release. */}}
{{- define "kp.componentName" -}}
{{- printf "%s-%s" (include "kp.fullname" .ctx) .component | trunc 63 | trimSuffix "-" -}}
{{- end -}}
