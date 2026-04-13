// Cloudflare Worker: payos-payment
// Format: addEventListener (tương thích mọi version Cloudflare Dashboard)
//
// KV Binding: PAYOS_STATUS
// Secrets: PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY, WORKER_BASE_URL

addEventListener("fetch", event => {
  event.respondWith(handleRequest(event.request));
});

async function handleRequest(request) {
  const url = new URL(request.url);
  const path = url.pathname;
  const method = request.method;

  // CORS
  if (method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type"
      }
    });
  }

  // ── /webhook → xử lý MỌI method (GET ping + POST thật) ──────────────────────
  if (path === "/webhook" || path === "/webhook/") {
    if (method === "GET" || method === "HEAD") {
      return ok({ success: true, status: "Webhook endpoint is active" });
    }

    // POST: nhận callback từ PayOS
    try {
      let body = {};
      try { body = await request.json(); } catch {
        return ok({ error: "Invalid JSON" }, 400);
      }

      const { code, data, signature: receivedSig } = body;

      if (!data || !receivedSig) {
        // Test ping không có body → OK
        return ok({ success: true });
      }

      if (code !== "00") {
        return ok({ success: true, desc: "Webhook ignored non-success code" });
      }

      // Verify HMAC-SHA256
      const signFields = [
        "accountNumber", "amount", "description", "orderCode",
        "reference", "transactionDateTime", "virtualAccountName", "virtualAccountNumber"
      ];
      const signString = signFields
        .filter(f => data[f] !== undefined && data[f] !== null)
        .map(f => `${f}=${data[f]}`)
        .join("&");

      const computed = await hmacSHA256(signString, PAYOS_CHECKSUM_KEY);
      if (computed !== receivedSig) {
        return ok({ error: "Invalid signature" }, 400);
      }

      // Ghi KV khi hợp lệ
      await PAYOS_STATUS.put(`paid_${data.orderCode}`, "paid", { expirationTtl: 86400 });

      // Cập nhật Firestore để Android Nhân Viên (Staff) nhận cảnh báo lập tức (không cần khách mở lại App)
      try {
        const mappedId = await PAYOS_STATUS.get(`map_${data.orderCode}`);
        const actualOrderId = mappedId ? parseInt(mappedId) : Math.floor(data.orderCode / 10000);

        const projectId = "android-app-4ba50";
        const apiKey = "AIzaSyANyV3YLPanlM8ogZE96BuHud1FYrgYfrQ";
        const fsUrl = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/orders/${actualOrderId}?updateMask.fieldPaths=payment_status&updateMask.fieldPaths=order_status&key=${apiKey}`;
        
        await fetch(fsUrl, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            fields: {
              payment_status: { stringValue: "paid" },
              order_status: { stringValue: "completed" }
            }
          })
        });
      } catch (e) {
          // FS update failed, but webhook succeeds
      }

      return ok({ success: true });

    } catch (e) {
      return ok({ error: "Webhook parse error", desc: e.message }, 500);
    }
  }

  // ── POST /create → Tạo link PayOS ────────────────────────────────────────────
  if (path === "/create" && method === "POST") {
    try {
      const body = await request.json();
      const { order_id, amount } = body;

      if (!order_id || !amount || parseInt(amount) <= 0) {
        return ok({ error: "Missing order_id or amount" }, 400);
      }

      const amountInt  = parseInt(amount);
      const orderCode  = parseInt(order_id) * 10000 + (Math.floor(Date.now() / 1000) % 10000);
      const description = `Thanh toan don ${order_id}`.substring(0, 25);
      const base       = WORKER_BASE_URL || `https://${url.hostname}`;
      const returnUrl  = `${base}/return`;
      const cancelUrl  = `${base}/cancel`;

      const signStr  = `amount=${amountInt}&cancelUrl=${cancelUrl}&description=${description}&orderCode=${orderCode}&returnUrl=${returnUrl}`;
      const signature = await hmacSHA256(signStr, PAYOS_CHECKSUM_KEY);

      // Lưu lại map để không phải chia orderCode hên xui
      await PAYOS_STATUS.put(`map_${orderCode}`, order_id.toString(), { expirationTtl: 86400 });

      const resp = await fetch("https://api-merchant.payos.vn/v2/payment-requests", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-client-id": PAYOS_CLIENT_ID,
          "x-api-key":   PAYOS_API_KEY
        },
        body: JSON.stringify({
          orderCode, amount: amountInt, description,
          returnUrl, cancelUrl, signature,
          items: [{ name: `Hoa don #${order_id}`, quantity: 1, price: amountInt }]
        })
      });

      const result = await resp.json();
      if (result.code !== "00" || !result.data || !result.data.checkoutUrl) {
        return ok({ error: `PayOS: ${result.desc || "Missing checkoutUrl"}` }, 502);
      }

      return ok({
        status: "payos_redirect",
        checkout_url: result.data.checkoutUrl,
        order_code: orderCode
      });

    } catch (e) {
      return ok({ error: e.message }, 500);
    }
  }

  // ── GET /status?order_code=xxx → Polling từ app ──────────────────────────────
  if (path === "/status" && method === "GET") {
    const orderCode = url.searchParams.get("order_code");
    if (!orderCode) return ok({ error: "Missing order_code" }, 400);
    const val = await PAYOS_STATUS.get(`paid_${orderCode}`);
    
    // Thêm Cache-Control giảm spam lên Worker/KV từ app
    return new Response(JSON.stringify({ paid: val === "paid", order_code: orderCode }), {
      status: 200,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Cache-Control": "public, max-age=3" 
      }
    });
  }

  // ── /return ───────────────────────────────────────────────────────────────────
  if (path === "/return") {
    const oc = url.searchParams.get("orderCode") || "";
    return html(`<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Thanh toán thành công</title>
<style>
body{font-family:sans-serif;text-align:center;padding:60px 20px;background:#f0faf0}
.card{background:#fff;max-width:380px;margin:auto;padding:40px 24px;border-radius:16px;box-shadow:0 4px 20px rgba(0,0,0,.1)}
.icon{font-size:64px}h2{color:#2e7d32}p{color:#666;font-size:14px}
.btn{display:inline-block;margin-top:20px;padding:12px 24px;background:#2e7d32;color:#fff;text-decoration:none;border-radius:8px;font-weight:bold;}
</style></head>
<body><div class="card"><div class="icon">✅</div>
<h2>Thanh toán thành công!</h2>
<p>Đơn <strong>#${oc}</strong> đã ghi nhận thành công.</p>
<p>Đang quay trở lại ứng dụng...</p>
<a href="restaurantapp://payment/success" class="btn">Mở lại ứng dụng ngay</a>
</div>
<script>
  setTimeout(function() {
    window.location.href = "restaurantapp://payment/success";
  }, 1500);
</script>
</body></html>`);
  }

  // ── /cancel ───────────────────────────────────────────────────────────────────
  if (path === "/cancel") {
    const oc = url.searchParams.get("orderCode") || "";
    return html(`<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Đã huỷ</title>
<style>
body{font-family:sans-serif;text-align:center;padding:60px 20px;background:#fff8f0}
.card{background:#fff;max-width:380px;margin:auto;padding:40px 24px;border-radius:16px;box-shadow:0 4px 20px rgba(0,0,0,.1)}
.icon{font-size:64px}h2{color:#c62828}p{color:#666;font-size:14px}
.btn{display:inline-block;margin-top:20px;padding:12px 24px;background:#c62828;color:#fff;text-decoration:none;border-radius:8px;font-weight:bold;}
</style></head>
<body><div class="card"><div class="icon">❌</div>
<h2>Đã huỷ thanh toán</h2>
<p>Giao dịch <strong>#${oc}</strong> bị huỷ.</p>
<p>Đang quay trở lại ứng dụng...</p>
<a href="restaurantapp://payment/cancel" class="btn">Quay lại ứng dụng</a>
</div>
<script>
  setTimeout(function() {
    window.location.href = "restaurantapp://payment/cancel";
  }, 1500);
</script>
</body></html>`);
  }

  // Health check root
  if (path === "/" || path === "") {
    return ok({ status: "ok", worker: "payos-payment" });
  }

  return ok({ error: "Not found" }, 404);
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function ok(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
  });
}

function html(body) {
  return new Response(body, { headers: { "Content-Type": "text/html;charset=UTF-8" } });
}

async function hmacSHA256(message, secret) {
  const enc = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw", enc.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false, ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, enc.encode(message));
  return Array.from(new Uint8Array(sig)).map(b => b.toString(16).padStart(2, "0")).join("");
}
