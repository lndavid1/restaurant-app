/**
 * Cloudflare Worker Proxy for Pexels API
 * Giúp giấu API Key khỏi mã nguồn Android App.
 *
 * Cách sử dụng cho App:
 * GET https://<your-worker-url>/?q=vietnamese%20pho
 *
 * App sẽ nhận về JSON chứa đúng link ảnh tốt nhất.
 */

const PEXELS_API_KEY = "6vkF29CZbGVIcAez1u4z8Cop3oPe8K3CPTGUw6t6hF9ciyhIs0U6MdT9";

export default {
  async fetch(request, env, ctx) {
    // 1. Chỉ chấp nhận parse param 'q' (từ khóa)
    const url = new URL(request.url);
    const keyword = url.searchParams.get("q");

    if (!keyword) {
      return new Response(JSON.stringify({ error: "Missing keyword parameter 'q'" }), {
        status: 400,
        headers: { "Content-Type": "application/json" }
      });
    }

    try {
      // 2. Fetch lên hệ thống Pexels (chỉ báo lấy đúng 1 dòng ảnh để siêu tiết kiệm băng thông)
      const pexelsUrl = `https://api.pexels.com/v1/search?query=${encodeURIComponent(keyword)}&per_page=1&orientation=landscape`;
      
      const pexelsResponse = await fetch(pexelsUrl, {
        method: "GET",
        headers: {
          "Authorization": PEXELS_API_KEY
        }
      });

      if (!pexelsResponse.ok) {
        return new Response(JSON.stringify({ error: "Thương lượng Pexels thất bại" }), { status: 500 });
      }

      const pexelsData = await pexelsResponse.json();

      // 3. Trích xuất đúng 1 URL ảnh độ phân giải trung bình (medium) để load nhanh trên App
      let finalImageUrl = null;
      if (pexelsData.photos && pexelsData.photos.length > 0) {
        finalImageUrl = pexelsData.photos[0].src.medium; // size medium (~350x200) là cực mượt cho list view
      }

      // 4. Trả kết quả về cho Android App
      return new Response(JSON.stringify({ 
        keyword: keyword,
        imageUrl: finalImageUrl 
      }), {
        status: 200,
        headers: { 
          "Content-Type": "application/json",
          // Thêm CORS để dev thoải mái
          "Access-Control-Allow-Origin": "*" 
        }
      });

    } catch (e) {
      return new Response(JSON.stringify({ error: e.message }), {
        status: 500,
        headers: { "Content-Type": "application/json" }
      });
    }
  }
};
