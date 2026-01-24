// WebSocket 클라이언트 관리 클래스
class TournamentWebSocket {
    constructor(tournamentId) {
        this.tournamentId = tournamentId;
        this.stompClient = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
        this.isConnected = false;
        this.subscriptions = [];
    }

    connect() {
        console.log(`[WebSocket] Connecting to tournament ${this.tournamentId}...`);
        
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.debug = (msg) => {
            // console.log('[STOMP]', msg);
        };

        const connectCallback = (frame) => {
            console.log('[WebSocket] ✅ Connected:', frame);
            this.isConnected = true;
            this.reconnectAttempts = 0;
            
            const subscription = this.stompClient.subscribe(
                `/topic/tournament/${this.tournamentId}`,
                this.handleMessage.bind(this)
            );
            this.subscriptions.push(subscription);
            
            this.showConnectionStatus('connected');
        };

        const errorCallback = (error) => {
            console.error('[WebSocket] ❌ Connection error:', error);
            this.isConnected = false;
            this.showConnectionStatus('disconnected');
            this.attemptReconnect();
        };

        this.stompClient.connect({}, connectCallback, errorCallback);
    }

    handleMessage(message) {
        console.log('[WebSocket] 📩 Message received:', message.body);
        
        try {
            const data = JSON.parse(message.body);
            console.log('[WebSocket] Parsed data:', data);
            
            switch (data.type) {
                case 'MATCH_STARTED':
                    this.onMatchStarted(data);
                    break;
                case 'MATCH_COMPLETED':
                    this.onMatchCompleted(data);
                    break;
                case 'BRACKET_UPDATED':
                    this.onBracketUpdated(data);
                    break;
                case 'TABLE_UPDATED':
                    this.onTableUpdated(data);
                    break;
                default:
                    console.warn('[WebSocket] Unknown message type:', data.type);
            }
            
            this.showToast(data.message, data.type);
            
        } catch (e) {
            console.error('[WebSocket] Failed to parse message:', e);
        }
    }

    onMatchStarted(data) {
        console.log('[WebSocket] 🔴 Match started:', data);
        
        const scheduledCard = $(`#scheduledMatchesContainer .match-card[data-match-id="${data.matchId}"]`);
        if (scheduledCard.length > 0) {
            scheduledCard.fadeOut(300, function() {
                $(this).remove();
                setTimeout(() => location.reload(), 500);
            });
        }
    }

    onMatchCompleted(data) {
        console.log('[WebSocket] ✅ Match completed:', data);
        
        const inProgressCard = $(`#inProgressMatchesContainer .match-card[data-match-id="${data.matchId}"]`);
        if (inProgressCard.length > 0) {
            inProgressCard.addClass('match-completed-animation');
            setTimeout(() => location.reload(), 1000);
        }
    }

    onBracketUpdated(data) {
        console.log('[WebSocket] 🎯 Bracket updated:', data);
        
        if (window.location.pathname.includes('/mainBracket')) {
            setTimeout(() => location.reload(), 500);
        }
    }

    onTableUpdated(data) {
        console.log('[WebSocket] 🏓 Table status updated:', data);
        
        if ($('#startMatchModal').hasClass('show')) {
            if (typeof loadTableList === 'function') {
                loadTableList();
            }
        }
    }

    showToast(message, type) {
        const bgClass = type === 'MATCH_COMPLETED' ? 'bg-success' : 
                       type === 'MATCH_STARTED' ? 'bg-warning' : 'bg-info';
        
        const toast = $(`
            <div class="toast align-items-center text-white ${bgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `);
        
        $('#toastContainer').append(toast);
        const bsToast = new bootstrap.Toast(toast[0], { delay: 5000 });
        bsToast.show();
        
        setTimeout(() => toast.remove(), 6000);
    }

    showConnectionStatus(status) {
        const badge = $('#wsConnectionBadge');
        if (status === 'connected') {
            badge.removeClass('bg-danger').addClass('bg-success').text('🟢 실시간 연결');
        } else {
            badge.removeClass('bg-success').addClass('bg-danger').text('🔴 연결 끊김');
        }
    }

    attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`[WebSocket] Reconnecting... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
            setTimeout(() => this.connect(), this.reconnectDelay);
        } else {
            console.error('[WebSocket] Max reconnect attempts reached. Please refresh the page.');
            this.showToast('실시간 연결이 끊어졌습니다. 페이지를 새로고침해주세요.', 'error');
        }
    }

    disconnect() {
        if (this.stompClient && this.isConnected) {
            this.subscriptions.forEach(sub => sub.unsubscribe());
            this.stompClient.disconnect(() => {
                console.log('[WebSocket] Disconnected');
            });
        }
    }
}

let tournamentWebSocket = null;

$(document).ready(function() {
    if (typeof TOURNAMENT_ID !== 'undefined' && TOURNAMENT_ID) {
        tournamentWebSocket = new TournamentWebSocket(TOURNAMENT_ID);
        tournamentWebSocket.connect();
    }
});

$(window).on('beforeunload', function() {
    if (tournamentWebSocket) {
        tournamentWebSocket.disconnect();
    }
});