import { useState } from "react";

export default function SimpleTextDisplay() {
  const API_URL = process.env.REACT_APP_API_URL

  const [text, setText] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);

  const [placeInfo, setPlaceInfo] = useState(null);
  const [corkageStatus, setCorkageStatus] = useState("");

  // 읍/면/동 정보 추출 함수
  const getTownInfoFromAddress = (address) => {
    try {
        const tokens = address.split(" ");
        return tokens.length >= 3 ? tokens[2] : ""; // 세 번째 토큰 추출 (예외 처리 포함)
    } catch (error) {
        console.error("주소 파싱 중 오류:", error);
        return "";
    }
  };

  const searchPlaces = async () => {
    if (!text.trim()) return;
    setLoading(true);

    try {
      const response = await fetch(
        `${API_URL}/api/search?query=${text}&page=1`
      );
      const data = await response.json();

      setResults(data.results || []);
    } catch (error) {
      console.error("검색 중 오류 발생:", error);
    } finally {
      setLoading(false);
    }
  };

  const savePlaceToDB = async (place, placeInfo, placeUrl, placeDesc) => {
    try {
      const response = await fetch(`${API_URL}/api/savePlace`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ place, placeInfo, placeUrl, placeDesc }),
        mode: 'cors', 
        credentials: 'include' // 쿠키 사용 시 필요       
      });
  
      if (response.ok) {
        const message = await response.text(); // 서버에서 받은 메시지 읽기
        alert(message); // 저장/업데이트 여부에 따른 메시지 출력
      } else {
        alert("데이터 저장에 실패했습니다.");
      }
    } catch (error) {
      console.error("데이터 저장 오류:", error);
      alert("저장 중 오류 발생");
    }
  };

  const getPlaceUrl = async (place) => {
    setLoading(true);
    const cleanTitle = place.title.replace(/<[^>]+>/g, "");
    const townInfo = getTownInfoFromAddress(place.address);

    try {
      const response = await fetch(
        `${API_URL}/api/getplaceurl?query=${encodeURIComponent(cleanTitle + " " + townInfo)}`
      );
      const data = await response.json();

      if (data.placeInfo) {
        setPlaceInfo(data.placeInfo);
        await savePlaceToDB(place, data.placeInfo, data.placeUrl, data.placeDesc); // 🔥 RDS 저장 로직 추가        
      } else {
        alert("실시간 반영이 되지 않았습니다. 서버에 작업요청이 되었으니 추후 확인하시거나 새로고침 후 다시 시도해 주세요.");
      }
    } catch (error) {
      console.error("네이버 검색 오류:", error);
      alert("검색 중 오류 발생");
    } finally {
      setLoading(false);
    }
  };

  const handleDetailClick = (place) => {
    const cleanTitle = place.title.replace(/<[^>]+>/g, "");
    const townInfo = getTownInfoFromAddress(place.address);
    
    console.log("CLEAN TITLE: ", cleanTitle);
    console.log("TOWN INFO: ", townInfo);
    
    window.open(`https://map.naver.com/p/search/${cleanTitle} ${townInfo}`, "_blank");
  };

  return (
    <div style={{ maxWidth: "600px", margin: "50px auto", textAlign: "center" }}>
      <h1>상호 입력</h1>
      <input
        type="text"
        placeholder="상호명을 입력하세요 (지역명을 같이 쓰는 걸 추천합니다)"
        value={text}
        onChange={(e) => setText(e.target.value)}
        style={{ padding: "10px", width: "100%", marginBottom: "10px" }}
      />
      <button
        onClick={searchPlaces}
        disabled={loading}
        style={{ padding: "10px 20px", cursor: "pointer" }}
      >
        찾기
      </button>

      <div style={{ marginTop: "20px" }}>
        {results.map((place, index) => (
          <div
            key={index}
            style={{
              border: "1px solid #ddd",
              borderRadius: "8px",
              padding: "15px",
              marginBottom: "15px",
              boxShadow: "0 4px 8px rgba(0, 0, 0, 0.1)",
              textAlign: "left",
              backgroundColor: "#fff",
            }}
          >
            <h2
              style={{ fontSize: "20px", marginBottom: "8px", fontWeight: "bold", color: "#333", lineHeight: "1.2"}}
              dangerouslySetInnerHTML={{ __html: place.title }}
            />
            <div
              style={{
                backgroundColor: "#f9f9f9", 
                padding:"8px 12px", 
                borderRadius:"8px", 
                marginBottom:"12px", 
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
              }}
            >
              <p style={{
                margin: "4px 0",
                color: "#555",
                fontSize: "14px",
                lineHeight: "1.4",
                whiteSpace: "nowrap",
                overflow: "hidden",
                textOverflow: "ellipsis"
              }}>{place.address}</p>
              <p style={{ 
                margin: "4px 0",
                color: "#444",
                fontWeight: "500",
                backgroundColor: "#eaeaea",
                padding: "4px 6px",
                borderRadius: "6px",
                display: "inline-block",
                fontSize: "14px",
                lineHeight: "1.4",
                whiteSpace: "nowrap",
                overflow: "hidden",
                textOverflow: "ellipsis" 
              }}>{place.roadAddress}</p>
            </div>
            <button
              onClick={() => handleDetailClick(place)}
              style={{
                padding: "8px 12px",
                marginRight: "10px",
                border: "none",
                borderRadius: "4px",
                backgroundColor: "#4CAF50",
                color: "#fff",
                cursor: "pointer",
              }}
            >
              자세히 보기
            </button>
            <button
              onClick={() => getPlaceUrl(place)}
              style={{
                padding: "8px 12px",
                border: "none",
                borderRadius: "4px",
                backgroundColor: "#2196F3",
                color: "#fff",
                cursor: "pointer",
              }}
            >
              등록 요청 하기
            </button>
          </div>
        ))}
      </div>

      {/* 🔥 전체 화면 로딩 모달 */}
      {loading && (
        <div style={{
          position: "fixed",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          backgroundColor: "rgba(0, 0, 0, 0.5)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          zIndex: 9999,
        }}>
          <div style={{
            padding: "20px",
            backgroundColor: "#fff",
            borderRadius: "8px",
            boxShadow: "0 4px 8px rgba(0, 0, 0, 0.2)",
          }}>
            <p style={{ margin: 0, fontSize: "18px" }}>확인 중...(최대 10초까지 걸려요)</p>
          </div>
        </div>
      )}
    </div>
  );
}
