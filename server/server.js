const express = require('express');
const os = require('os');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const app = express();
const PORT = process.env.PORT || 3000;

// ═══════════════════════════════════════════════════════════════
// JSON File Database
// ═══════════════════════════════════════════════════════════════
const DB_PATH = path.join(__dirname, 'db.json');

function loadDB() {
    try {
        if (fs.existsSync(DB_PATH)) {
            return JSON.parse(fs.readFileSync(DB_PATH, 'utf8'));
        }
    } catch (e) {
        console.error('⚠️ DB read error, resetting:', e.message);
    }
    return {
        devices: {},          // tracker_id → device info + token
        sms_log: [],          // all forwarded SMS
        sms_senders: [],      // configured SMS senders/webhooks
        accounts: {},         // tracker_id → account data
        requests_log: []      // all raw requests log
    };
}

function saveDB(db) {
    try {
        fs.writeFileSync(DB_PATH, JSON.stringify(db, null, 2), 'utf8');
    } catch (e) {
        console.error('⚠️ DB write error:', e.message);
    }
}

let db = loadDB();

// ═══════════════════════════════════════════════════════════════
// Middleware
// ═══════════════════════════════════════════════════════════════
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.text());

// ═══════════════════════════════════════════════════════════════
// Request Logging Middleware
// ═══════════════════════════════════════════════════════════════
app.use((req, res, next) => {
    const timestamp = new Date().toLocaleString();
    console.log(`\n${'═'.repeat(60)}`);
    console.log(`📡 [${timestamp}] ${req.method} ${req.originalUrl}`);
    console.log(`   Content-Type: ${req.get('Content-Type') || 'none'}`);
    console.log(`   User-Agent:   ${req.get('User-Agent') || 'none'}`);
    console.log(`   From IP:      ${req.headers['x-forwarded-for'] || req.ip}`);
    next();
});

// ═══════════════════════════════════════════════════════════════
// GET / — Server Status Page
// ═══════════════════════════════════════════════════════════════
app.get('/', (req, res) => {
    const uptime = process.uptime();
    const hours = Math.floor(uptime / 3600);
    const mins = Math.floor((uptime % 3600) / 60);
    const secs = Math.floor(uptime % 60);

    res.json({
        status: 'running',
        server: 'PipraPay Debug Server v3.0',
        uptime: `${hours}h ${mins}m ${secs}s`,
        connectedDevices: Object.keys(db.devices).length,
        totalSmsReceived: db.sms_log.length,
        totalAccounts: Object.keys(db.accounts).length,
        dbFile: DB_PATH
    });
});

// ═══════════════════════════════════════════════════════════════
// GET /devices — List Connected Devices
// ═══════════════════════════════════════════════════════════════
app.get('/devices', (req, res) => {
    res.json({ count: Object.keys(db.devices).length, devices: db.devices });
});

// ═══════════════════════════════════════════════════════════════
// GET /sms — List Received SMS Messages
// ═══════════════════════════════════════════════════════════════
app.get('/sms', (req, res) => {
    res.json({ count: db.sms_log.length, messages: db.sms_log.slice(-50) });
});

// ═══════════════════════════════════════════════════════════════
// GET /accounts — List All Accounts
// ═══════════════════════════════════════════════════════════════
app.get('/accounts', (req, res) => {
    res.json({ count: Object.keys(db.accounts).length, accounts: db.accounts });
});

// ═══════════════════════════════════════════════════════════════
// GET /logs — Raw Request Log
// ═══════════════════════════════════════════════════════════════
app.get('/logs', (req, res) => {
    res.json({ count: db.requests_log.length, logs: db.requests_log.slice(-100) });
});

// ═══════════════════════════════════════════════════════════════
// GET /db — Full Database Dump
// ═══════════════════════════════════════════════════════════════
app.get('/db', (req, res) => {
    res.json(db);
});

// ═══════════════════════════════════════════════════════════════
// POST * — Catch-All Handler
// ═══════════════════════════════════════════════════════════════
app.post('*', (req, res) => {
    const contentType = req.get('Content-Type') || '';
    const body = req.body;
    const action = body ? body['action-companion'] : null;

    console.log(`   Body:`, typeof body === 'object' ? JSON.stringify(body, null, 2) : body);

    // Log every request to DB
    db.requests_log.push({
        timestamp: new Date().toISOString(),
        method: req.method,
        path: req.originalUrl,
        contentType,
        userAgent: req.get('User-Agent'),
        ip: req.headers['x-forwarded-for'] || req.ip,
        body: body
    });

    // ══════════════════════════════════════════════════════════
    // PayRyzen/Companion App API (action-companion based)
    // ══════════════════════════════════════════════════════════

    if (action) {
        return handleCompanionAction(action, body, req, res);
    }

    // ══════════════════════════════════════════════════════════
    // PipraPay Original API (connection_status / check based)
    // ══════════════════════════════════════════════════════════

    // 1. CONNECTION REGISTRATION
    if (body && body.connection_status === 'Connected') {
        const deviceKey = `${body.d_brand || 'unknown'}_${body.d_model || 'unknown'}`;
        db.devices[deviceKey] = {
            brand: body.d_brand || 'N/A',
            model: body.d_model || 'N/A',
            androidVersion: body.d_version || 'N/A',
            apiLevel: body.d_api_level || 'N/A',
            connectedAt: new Date().toISOString(),
            lastSeen: new Date().toISOString(),
            source: 'piprapay'
        };
        saveDB(db);

        console.log(`\n   🟢 PIPRAPAY CONNECTED: ${body.d_brand} ${body.d_model}`);

        return res.status(200).json({
            status: "true",
            message: "🚀 Successfully Connected to PipraPay Debug Server!"
        });
    }

    // 2. DISCONNECTION
    if (body && body.connection_status === 'Disconnected') {
        const deviceKey = `${body.d_brand || 'unknown'}_${body.d_model || 'unknown'}`;
        delete db.devices[deviceKey];
        saveDB(db);

        console.log(`\n   🔴 PIPRAPAY DISCONNECTED: ${body.d_brand} ${body.d_model}`);

        return res.status(200).json({
            status: "true",
            message: "Webhook removed successfully"
        });
    }

    // 3. KEEP-ALIVE PING
    if (body && body.check === 'i_am_active') {
        const deviceKey = `${body.d_brand || 'unknown'}_${body.d_model || 'unknown'}`;
        if (db.devices[deviceKey]) {
            db.devices[deviceKey].lastSeen = new Date().toISOString();
            saveDB(db);
        }

        console.log(`\n   ⚡ KEEP-ALIVE: ${body.d_brand} ${body.d_model}`);

        return res.status(200).send("OK");
    }

    // 4. SMS FORWARD (JSON payload from WorkManager)
    if (contentType.includes('application/json') && body && body.from && body.text) {
        console.log(`\n   💬 ═══ INCOMING SMS ═══`);
        logSmsDetails(body);

        db.sms_log.push({
            ...body,
            receivedAt: new Date().toISOString(),
            endpoint: req.originalUrl,
            source: 'piprapay-workmanager'
        });
        saveDB(db);

        return res.status(200).json({
            status: "success",
            message: "SMS forwarded and logged successfully"
        });
    }

    // 5. FALLBACK
    console.log(`\n   ❓ UNRECOGNIZED REQUEST — saved to db.requests_log`);
    saveDB(db);

    return res.status(200).json({
        status: "true",
        message: "Request received and logged"
    });
});

// ═══════════════════════════════════════════════════════════════
// PayRyzen Companion App — Action Handler
// ═══════════════════════════════════════════════════════════════
function handleCompanionAction(action, body, req, res) {
    console.log(`\n   🔷 COMPANION ACTION: ${action}`);

    switch (action) {

        // ─────────────────────────────────────────────────────
        // LOGIN — Device registration + OTP verification
        // ─────────────────────────────────────────────────────
        case 'login': {
            const { onetimepassword, app_version, tracker_id, name, model, android_level } = body;

            console.log(`   ┌─ OTP:        ${onetimepassword}`);
            console.log(`   ├─ Tracker ID: ${tracker_id}`);
            console.log(`   ├─ Device:     ${name} (${model})`);
            console.log(`   └─ Android:    ${android_level} | App: v${app_version}`);

            // Generate auth token
            const token = crypto.randomBytes(32).toString('hex');

            // Save device to DB
            db.devices[tracker_id] = {
                tracker_id,
                name,
                model,
                android_level,
                app_version,
                otp_used: onetimepassword,
                token,
                loggedInAt: new Date().toISOString(),
                lastSeen: new Date().toISOString(),
                source: 'payryzen-companion',
                ip: req.headers['x-forwarded-for'] || req.ip
            };

            // Create account entry with default wildcard sender
            if (!db.accounts[tracker_id]) {
                db.accounts[tracker_id] = {
                    tracker_id,
                    device_name: name,
                    model,
                    balance: "0.00",
                    currency: "BDT",
                    status: "active",
                    created_at: new Date().toISOString(),
                    sms_forwarded: 0,
                    pending: 0,
                    stored: 0,
                    used: 0,
                    error: 0,
                    sms_senders: ["*"]
                };
            }

            // Also set global sms_senders default if empty
            if (db.sms_senders.length === 0) {
                db.sms_senders = ["*"];
            }

            saveDB(db);

            console.log(`   ✅ Login successful, token issued`);

            return res.status(200).json({
                status: "true",
                message: "Login successful",
                token: token,
                tracker_id: tracker_id
            });
        }

        // ─────────────────────────────────────────────────────
        // ACCOUNT INFORMATION — Return account data
        // ─────────────────────────────────────────────────────
        case 'account-information': {
            const { token } = body;
            console.log(`   ┌─ Token: ${token ? token.substring(0, 16) + '...' : 'empty'}`);

            // Find device by token
            const device = Object.values(db.devices).find(d => d.token === token);
            const trackerId = device ? device.tracker_id : null;
            const account = trackerId ? db.accounts[trackerId] : null;

            if (device) {
                device.lastSeen = new Date().toISOString();
                saveDB(db);
            }

            // Count SMS by status for this device
            const deviceSms = db.sms_log.filter(s => {
                if (trackerId && s.tracker_id === trackerId) return true;
                if (token && s.token && s.token.startsWith(token.substring(0, 16))) return true;
                return false;
            });
            const pendingCount = deviceSms.filter(s => s.status === 'pending').length;
            const storedCount = deviceSms.filter(s => s.status === 'stored' || !s.status).length;
            const usedCount = deviceSms.filter(s => s.status === 'used').length;
            const errorCount = deviceSms.filter(s => s.status === 'error').length;

            console.log(`   └─ Account: ${account ? 'found' : 'default'} | SMS: ${deviceSms.length} total`);

            const accountData = account ? {
                ...account,
                sms_forwarded: deviceSms.length || account.sms_forwarded || 0,
                pending: pendingCount,
                stored: storedCount || deviceSms.length,
                used: usedCount,
                error: errorCount,
                sms_log: deviceSms.slice(-20),
                sms_senders: account.sms_senders || ["*"]
            } : {
                tracker_id: "unknown",
                device_name: "Unknown Device",
                model: "unknown",
                balance: "0.00",
                currency: "BDT",
                status: "active",
                sms_forwarded: db.sms_log.length,
                pending: 0,
                stored: db.sms_log.length,
                used: 0,
                error: 0,
                sms_log: db.sms_log.slice(-20),
                sms_senders: db.sms_senders.length > 0 ? db.sms_senders : ["*"]
            };

            return res.status(200).json({
                status: "true",
                message: "Account information retrieved",
                data: accountData
            });
        }

        // ─────────────────────────────────────────────────────
        // SMS TRANSMIT SENDER — Return configured SMS senders
        // ─────────────────────────────────────────────────────
        case 'sms-transmit-sender': {
            const { token: smsToken } = body;
            console.log(`   ┌─ Token: ${smsToken ? smsToken.substring(0, 16) + '...' : 'empty'}`);

            // Find device by token
            const smsDevice = Object.values(db.devices).find(d => d.token === smsToken);
            const smsTrackerId = smsDevice ? smsDevice.tracker_id : null;
            const smsAccount = smsTrackerId ? db.accounts[smsTrackerId] : null;

            // Default to wildcard "*" (capture ALL SMS) if no senders configured
            let senders = [];
            if (smsAccount && smsAccount.sms_senders && smsAccount.sms_senders.length > 0) {
                senders = smsAccount.sms_senders;
            } else if (db.sms_senders && db.sms_senders.length > 0) {
                senders = db.sms_senders;
            } else {
                senders = ["*"];  // Wildcard: capture all SMS
            }

            if (smsDevice) {
                smsDevice.lastSeen = new Date().toISOString();
                saveDB(db);
            }

            console.log(`   └─ Senders: ${senders.length} configured (${senders.join(', ')})`);

            return res.status(200).json({
                status: "true",
                message: "SMS sender list retrieved",
                senders: senders,
                data: senders,
                count: senders.length
            });
        }

        // ─────────────────────────────────────────────────────
        // SMS TRANSMIT — Forwarded SMS from companion app
        // ─────────────────────────────────────────────────────
        case 'sms-transmit': {
            const { token: txToken } = body;
            // Support multiple field names for SMS data
            const smsFrom = body.from || body.sender || body.sms_sender || 'unknown';
            const smsText = body.text || body.message || body.sms_body || body.sms_text || '';
            const smsSim = body.sim || body.sim_slot || 'unknown';
            const smsSent = body.sentStamp || body.sent_stamp || body.timestamp || Date.now();
            const smsReceived = body.receivedStamp || body.received_stamp || Date.now();

            console.log(`\n   💬 ═══ COMPANION SMS FORWARD ═══`);
            logSmsDetails({ from: smsFrom, text: smsText, sim: smsSim, sentStamp: smsSent, receivedStamp: smsReceived });

            // Find device tracker_id from token
            const txDevice = Object.values(db.devices).find(d => d.token === txToken);
            const txTrackerId = txDevice ? txDevice.tracker_id : null;

            const smsEntry = {
                from: smsFrom,
                text: smsText,
                sim: smsSim,
                sentStamp: smsSent,
                receivedStamp: smsReceived,
                receivedAt: new Date().toISOString(),
                source: 'payryzen-companion',
                tracker_id: txTrackerId,
                token: txToken ? txToken.substring(0, 16) + '...' : 'none',
                status: 'stored',
                // Store ALL original fields from the companion app
                raw: body
            };

            db.sms_log.push(smsEntry);

            // Update account sms count
            if (txDevice && db.accounts[txDevice.tracker_id]) {
                db.accounts[txDevice.tracker_id].sms_forwarded = (db.accounts[txDevice.tracker_id].sms_forwarded || 0) + 1;
                db.accounts[txDevice.tracker_id].stored = (db.accounts[txDevice.tracker_id].stored || 0) + 1;
            }

            saveDB(db);

            console.log(`   ✅ Total SMS logged: ${db.sms_log.length}`);

            return res.status(200).json({
                status: "true",
                message: "SMS received and logged"
            });
        }

        // ─────────────────────────────────────────────────────
        // ANY OTHER ACTION — log and accept
        // ─────────────────────────────────────────────────────
        default: {
            console.log(`   ⚠️ Unknown companion action: "${action}"`);
            console.log(`   Full body:`, JSON.stringify(body, null, 2));
            saveDB(db);

            return res.status(200).json({
                status: "true",
                message: `Action "${action}" received and logged`
            });
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Helper: Log SMS details
// ═══════════════════════════════════════════════════════════════
function logSmsDetails(sms) {
    console.log(`   ┌──────────────────────────────────────────`);
    console.log(`   │ 📱 From:      ${sms.from || sms.sender || 'N/A'}`);
    console.log(`   │ 📨 Message:   "${sms.text || sms.message || sms.sms_body || 'N/A'}"`);
    console.log(`   │ 📶 SIM:       ${sms.sim || 'undetected'}`);
    console.log(`   │ 🕒 Sent:      ${sms.sentStamp ? new Date(Number(sms.sentStamp)).toLocaleString() : 'N/A'}`);
    console.log(`   │ 📥 Received:  ${sms.receivedStamp ? new Date(Number(sms.receivedStamp)).toLocaleString() : 'N/A'}`);
    console.log(`   └──────────────────────────────────────────`);
}

// ═══════════════════════════════════════════════════════════════
// Get local network IPs
// ═══════════════════════════════════════════════════════════════
function getLocalIPs() {
    const interfaces = os.networkInterfaces();
    const ips = [];
    for (const name of Object.keys(interfaces)) {
        for (const iface of interfaces[name]) {
            if (iface.family === 'IPv4' && !iface.internal) {
                ips.push({ name, address: iface.address });
            }
        }
    }
    return ips;
}

// ═══════════════════════════════════════════════════════════════
// Start Server
// ═══════════════════════════════════════════════════════════════
const server = app.listen(PORT, '0.0.0.0', () => {
    const ips = getLocalIPs();
    console.log(`\n${'═'.repeat(60)}`);
    console.log(`🚀 PipraPay Debug Server v3.0`);
    console.log(`   Handles: PipraPay + PayRyzen Companion APIs`);
    console.log(`   Database: ${DB_PATH}`);
    console.log(`${'═'.repeat(60)}`);
    console.log(`\n📍 Local:     http://localhost:${PORT}`);
    ips.forEach(ip => {
        console.log(`📍 Network:   http://${ip.address}:${PORT}  (${ip.name})`);
    });
    console.log(`\n📌 Endpoints:`);
    console.log(`   GET  /          → Server status`);
    console.log(`   GET  /devices   → Connected devices`);
    console.log(`   GET  /sms       → SMS log`);
    console.log(`   GET  /accounts  → Account data`);
    console.log(`   GET  /logs      → Raw request log`);
    console.log(`   GET  /db        → Full database dump`);
    console.log(`   POST /*         → All app requests`);
    console.log(`\n${'═'.repeat(60)}`);
    console.log(`⏳ Waiting for connections...\n`);
});

server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
        console.error(`\n❌ Port ${PORT} is already in use!`);
        console.error(`   Fix: npx kill-port ${PORT}   OR   SET PORT=3001 && npm start\n`);
    } else {
        console.error(`\n❌ Server error:`, err);
    }
    process.exit(1);
});

process.on('uncaughtException', (err) => {
    console.error(`\n❌ Uncaught exception:`, err);
});

process.on('SIGINT', () => {
    console.log(`\n👋 Server shutting down... DB saved.`);
    saveDB(db);
    server.close();
    process.exit(0);
});
