/**
 * app.js — Frontend logic for Thread Visualizer
 *
 * What this does:
 *   1. Connects to Spring Boot via WebSocket (STOMP over SockJS)
 *   2. Subscribes to /topic/threads — receives JSON array every 1 second
 *   3. Draws thread nodes as colored circles inside the process circle
 *   4. Updates sidebar: thread list + selected thread detail
 *   5. Maintains an append-only history log of state changes
 */

// ── State ─────────────────────────────────────────────────────
let stompClient = null;
let selectedThreadId = null;
let historyLog = [];           // append-only array of state change events
let previousStates = {};       // { threadId: state } — used to detect changes

// ── 1. WebSocket Connection ────────────────────────────────────
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // disable STOMP debug logs in console

    stompClient.connect({}, onConnected, onError);
}

function onConnected() {
    setStatus(true);
    console.log('Connected to Spring Boot WebSocket');

    // Subscribe to the channel where Spring Boot sends thread data
    // Spring Boot sends here: messagingTemplate.convertAndSend("/topic/threads", threads)
    stompClient.subscribe('/topic/threads', function(message) {
        const threads = JSON.parse(message.body);
        renderAll(threads);
    });
}

function onError(error) {
    setStatus(false);
    console.error('WebSocket connection failed:', error);
    // Auto-retry after 3 seconds
    setTimeout(connect, 3000);
}

function reconnect() {
    if (stompClient) {
        try { stompClient.disconnect(); } catch(e) {}
    }
    setStatus(false);
    setTimeout(connect, 500);
}

// ── 2. Status indicator ────────────────────────────────────────
function setStatus(connected) {
    const dot  = document.getElementById('status-dot');
    const text = document.getElementById('status-text');
    dot.className = connected ? 'connected' : 'disconnected';
    text.textContent = connected ? 'Connected to JVM' : 'Disconnected';
}

// ── 3. Main render function — called every second ──────────────
function renderAll(threads) {
    updateStats(threads);
    renderProcessCircle(threads);
    renderThreadList(threads);
    detectStateChanges(threads);

    // Refresh detail card if a thread is selected
    if (selectedThreadId !== null) {
        const selected = threads.find(t => t.id === selectedThreadId);
        if (selected) renderDetailCard(selected);
    }
}

// ── 4. Stats bar ───────────────────────────────────────────────
function updateStats(threads) {
    document.getElementById('total-count').textContent = threads.length;
    document.getElementById('run-count').textContent =
        threads.filter(t => t.state === 'RUNNABLE').length;
    document.getElementById('block-count').textContent =
        threads.filter(t => t.state === 'BLOCKED').length;
    document.getElementById('wait-count').textContent =
        threads.filter(t => t.state === 'WAITING' || t.state === 'TIMED_WAITING').length;
}

// ── 5. Process circle — draw thread nodes ─────────────────────
function renderProcessCircle(threads) {
    const container = document.getElementById('thread-nodes');
    container.innerHTML = '';

    if (threads.length === 0) return;

    const circleSize = 360;   // must match CSS #process-circle width/height
    // Change center from 180 to 250 (matches new 500px circle)
    const center = 250;
    const nodeSize = 56;  // matches new node size
    const orbit = threads.length <= 4 ? 150 : Math.min(190, 430 / threads.length * 2);    // must match CSS .thread-node width/height
    const nodeRadius = nodeSize / 2;

    // Arrange thread nodes evenly around a circle inside the process circle
    // Radius = how far from center each node sits
    const orbitRadius = threads.length <= 4 ? 110 : Math.min(130, 300 / threads.length * 2);

    threads.forEach((thread, index) => {
        // Divide the circle evenly — start at top (-π/2) so first node is at 12 o'clock
        const angle = (2 * Math.PI * index / threads.length) - (Math.PI / 2);

        const x = center + orbitRadius * Math.cos(angle) - nodeRadius;
        const y = center + orbitRadius * Math.sin(angle) - nodeRadius;

        const node = document.createElement('div');
        node.className = 'thread-node ' + thread.state;
        if (thread.id === selectedThreadId) node.classList.add('selected');

        // Show first 6 chars of thread name — fits in the circle
        node.textContent = thread.name.length > 6
            ? thread.name.substring(0, 6) + '..'
            : thread.name;

        node.title = `${thread.name}\nState: ${thread.state}\nMethod: ${thread.currentMethod}`;
        node.style.left = x + 'px';
        node.style.top  = y + 'px';

        node.addEventListener('click', () => {
            selectedThreadId = thread.id;
            renderDetailCard(thread);
            // Update selected styling
            document.querySelectorAll('.thread-node').forEach(n => n.classList.remove('selected'));
            document.querySelectorAll('.thread-row').forEach(r => r.classList.remove('selected'));
            node.classList.add('selected');
            document.getElementById('row-' + thread.id)?.classList.add('selected');
        });

        container.appendChild(node);
    });
}

// ── 6. Sidebar thread list ─────────────────────────────────────
function renderThreadList(threads) {
    const list = document.getElementById('thread-list');
    list.innerHTML = '';

    threads.forEach(thread => {
        const row = document.createElement('div');
        row.className = 'thread-row' + (thread.id === selectedThreadId ? ' selected' : '');
        row.id = 'row-' + thread.id;

        row.innerHTML = `
            <span class="badge ${thread.state}">${thread.state.replace('_',' ')}</span>
            <span class="tname">${thread.name}</span>
            <span class="tmethod">${thread.currentMethod}</span>
        `;

        row.addEventListener('click', () => {
            selectedThreadId = thread.id;
            renderDetailCard(thread);
            document.querySelectorAll('.thread-row').forEach(r => r.classList.remove('selected'));
            document.querySelectorAll('.thread-node').forEach(n => n.classList.remove('selected'));
            row.classList.add('selected');
            document.getElementById('node-' + thread.id)?.classList.add('selected');
        });

        list.appendChild(row);
    });
}

// ── 7. Detail card (selected thread) ──────────────────────────
function renderDetailCard(thread) {
    document.getElementById('detail-header').textContent = 'Thread details';

    document.getElementById('detail-body').innerHTML = `
        <div class="detail-name">
            ${thread.name}
            <span class="badge ${thread.state}" style="margin-left:6px">${thread.state.replace('_',' ')}</span>
        </div>
        <div class="detail-row">
            <span class="label">Thread ID</span>
            <span class="value">${thread.id}</span>
        </div>
        <div class="detail-row">
            <span class="label">Thread group</span>
            <span class="value">${thread.threadGroup}</span>
        </div>
        <div class="detail-row">
            <span class="label">Current method</span>
            <span class="value" title="${thread.currentMethod}">${thread.currentMethod}</span>
        </div>
        <div class="detail-row">
            <span class="label">Source file</span>
            <span class="value">${thread.sourceFile}</span>
        </div>
        <div class="detail-row">
            <span class="label">Line number</span>
            <span class="value">${thread.lineNumber > 0 ? thread.lineNumber : '-'}</span>
        </div>
    `;
}

// ── 8. History log — append only, detects state changes ───────
function detectStateChanges(threads) {
    const now = new Date().toLocaleTimeString();
    threads.forEach(t => {
        if (previousStates[t.id] === undefined) {
            // First time seeing this thread — log it
            historyLog.unshift({ time: now, name: t.name, state: t.state });
        } else if (previousStates[t.id] !== t.state) {
            // State changed — log it
            historyLog.unshift({ time: now, name: t.name, state: t.state });
        }
        previousStates[t.id] = t.state;
        if (historyLog.length > 100) historyLog.pop();
    });
    renderHistory();
}

function renderHistory() {
    const list = document.getElementById('history-list');
    list.innerHTML = historyLog.slice(0, 50).map(entry => `
        <div class="history-entry">
            <span class="h-time">${entry.time}</span>
            <span class="h-name">${entry.name}</span>
            <span>→</span>
            <span class="h-state ${entry.state}">${entry.state.replace('_',' ')}</span>
        </div>
    `).join('');
}

function clearHistory() {
    historyLog = [];
    previousStates = {};
    renderHistory();
}

// ── Start everything ───────────────────────────────────────────
connect();