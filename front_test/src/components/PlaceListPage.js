import React, { useEffect, useState } from "react";

const API_URL = process.env.REACT_APP_API_URL;

export default function PlaceListPage() {
  const [places, setPlaces] = useState([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);

  // API 호출 함수
  const fetchPlaces = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/api/places?page=${page}&size=5`);
      if (response.ok) {
        const data = await response.json();
        if (data.content.length > 0) {
          setPlaces((prevPlaces) => [...prevPlaces, ...data.content]);
          setHasMore(!data.last); // 마지막 페이지인지 확인
        } else {
          setHasMore(false);
        }
      } else {
        console.error("데이터 불러오기 실패");
      }
    } catch (error) {
      console.error("API 호출 오류:", error);
    } finally {
      setLoading(false);
    }
  };

  // 컴포넌트가 처음 렌더링될 때 데이터 로드
  useEffect(() => {
    fetchPlaces();
  }, [page]);

  // 더보기 버튼 클릭 시 페이지 증가
  const handleLoadMore = () => {
    if (!loading && hasMore) {
      setPage((prevPage) => prevPage + 1);
    }
  };

  return (
    <div style={{ maxWidth: "600px", margin: "50px auto", textAlign: "center" }}>
      <h1>콜키지 가능한 장소 리스트</h1>
      
      <div style={{ marginTop: "20px" }}>
        {places.map((place) => (
          <div
            key={place.id}
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
            <h2 style={{ fontSize: "20px", fontWeight: "bold", color: "#333" }}>
              {place.title}
            </h2>
            <p style={{ margin: "8px 0", color: "#555" }}>{place.address}</p>
            
            <p style={{ margin: "8px 0", color: "#444" }}>
              콜키지 가능: {place.corkageAvailable ? "가능" : "불가능"}
            </p>
            
            <p style={{ margin: "8px 0", color: "#444" }}>
              콜키지 비용:{" "}
              {place.corkageAvailable
                ? place.freeCorkage
                  ? "무료"
                  : "유료"
                : "콜키지 불가능"}
            </p>

            {place.placeUrl && (
              <a
                href={place.placeUrl}
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: "#2196F3", textDecoration: "none" }}
              >
                상세보기
              </a>
            )}
          </div>
        ))}
      </div>

      {loading && <p>로딩 중...</p>}

      {!loading && hasMore && (
        <button
          onClick={handleLoadMore}
          style={{
            padding: "10px 20px",
            marginTop: "20px",
            border: "none",
            borderRadius: "4px",
            backgroundColor: "#4CAF50",
            color: "#fff",
            cursor: "pointer",
          }}
        >
          더보기
        </button>
      )}

      {!hasMore && <p>모든 데이터를 불러왔습니다.</p>}
    </div>
  );
}
