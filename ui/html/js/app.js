"use strict";

const $ = (id) => document.getElementById(id);

let conversationId = null;

function authHeader() {
  const [u, p] = $("user").value.split(":");
  $("whoami").textContent = `signed in as ${u}`;
  return "Basic " + btoa(`${u}:${p}`);
}

function setConversation(id) {
  conversationId = id;
  $("conversationId").textContent = id || "new";
}

$("resetConv").addEventListener("click", () => setConversation(null));

$("uploadForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const file = $("file").files[0];
  const status = $("uploadStatus");
  if (!file) return;
  status.className = "status";
  status.textContent = "Uploading…";
  const body = new FormData();
  body.append("file", file);
  try {
    const res = await fetch("/api/v1/documents", {
      method: "POST",
      headers: { Authorization: authHeader() },
      body,
    });
    if (res.status === 202) {
      const json = await res.json();
      status.className = "status ok";
      status.textContent = `Accepted (documentId ${json.id ?? "?"}). Indexing…`;
    } else if (res.status === 401 || res.status === 403) {
      status.className = "status error";
      status.textContent = `Not authorized (${res.status}). Only admin may upload.`;
    } else {
      status.className = "status error";
      status.textContent = `Upload failed (${res.status}): ${await res.text()}`;
    }
  } catch (err) {
    status.className = "status error";
    status.textContent = `Upload error: ${err}`;
  }
});

$("chatForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const question = $("question").value.trim();
  if (!question) return;

  const answerEl = $("answer");
  const sourcesEl = $("sources");
  const timingsEl = $("timings");
  const errorEl = $("chatError");
  answerEl.textContent = "";
  sourcesEl.innerHTML = "";
  timingsEl.textContent = "";
  errorEl.textContent = "";
  $("askBtn").disabled = true;

  try {
    const res = await fetch("/api/v1/rag/chat", {
      method: "POST",
      headers: {
        Authorization: authHeader(),
        "Content-Type": "application/json",
        Accept: "application/x-ndjson",
      },
      body: JSON.stringify({ conversationId, question }),
    });

    if (!res.ok) {
      errorEl.textContent = `Request failed (${res.status}): ${await res.text()}`;
      return;
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let nl;
      while ((nl = buffer.indexOf("\n")) >= 0) {
        const line = buffer.slice(0, nl).trim();
        buffer = buffer.slice(nl + 1);
        if (line) handleChunk(JSON.parse(line), { answerEl, sourcesEl, timingsEl, errorEl });
      }
    }
  } catch (err) {
    errorEl.textContent = `Stream error: ${err}`;
  } finally {
    $("askBtn").disabled = false;
  }
});

function handleChunk(chunk, els) {
  switch (chunk.type) {
    case "META":
      setConversation(chunk.conversationId);
      break;
    case "SOURCES":
      renderSources(chunk.sources || [], els.sourcesEl);
      break;
    case "DELTA":
      els.answerEl.textContent += chunk.content || "";
      break;
    case "DONE":
      els.timingsEl.textContent = JSON.stringify(chunk.timings, null, 2);
      break;
    case "ERROR":
      els.errorEl.textContent = chunk.content || "Unknown error";
      break;
  }
}

function renderSources(sources, el) {
  el.innerHTML = "";
  if (sources.length === 0) {
    el.innerHTML = "<li class='muted'>No relevant sources found.</li>";
    return;
  }
  for (const s of sources) {
    const li = document.createElement("li");
    const name = s.fileName ? escapeHtml(s.fileName) : `<code>${s.documentId}</code>`;
    const title = s.link
      ? `<a href="${escapeHtml(s.link)}" target="_blank" rel="noopener">${name}</a>`
      : name;
    li.innerHTML =
      `${title} · chunk ${s.chunkIndex} ` +
      `· <span class="score">score ${s.score.toFixed(3)}</span>` +
      `<span class="snippet">${escapeHtml(s.snippet)}</span>`;
    el.appendChild(li);
  }
}

function escapeHtml(str) {
  return (str || "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

authHeader();
