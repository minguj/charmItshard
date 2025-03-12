import React, { useEffect, useState } from "react";

const API_URL = process.env.REACT_APP_API_URL;

export default function PlaceListPage() {
  const [places, setPlaces] = useState([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [categories, setCategories] = useState([]);
  const [addresses, setAddresses] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [selectedCity, setSelectedCity] = useState("");
  const [selectedDistrict, setSelectedDistrict] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [subwaySearchTerm, setSubwaySearchTerm] = useState("");

  // 📤 API 호출 함수
  const fetchPlaces = async (reset = false) => {
    setLoading(true);

    const cleanedSubwaySearchTerm = subwaySearchTerm.endsWith('역') 
    ? subwaySearchTerm.slice(0, -1).trim() 
    : subwaySearchTerm.trim();

    try {
      const response = await fetch(
        `${API_URL}/api/places?page=${page}&size=5&category=${selectedCategory}&city=${selectedCity}&district=${selectedDistrict}&searchTerm=${searchTerm}&subway=${cleanedSubwaySearchTerm}`
      );
      if (response.ok) {
        const data = await response.json();
        console.log("📥 Received data from API:", data);
        setPlaces((prevPlaces) => (reset ? data.content : [...prevPlaces, ...data.content]));
        setHasMore(!data.last);
      } else {
        console.error("데이터 불러오기 실패");
      }
    } catch (error) {
      console.error("API 호출 오류:", error);
    } finally {
      setLoading(false);
    }
  };

  // 📥 카테고리 데이터 가져오기
  const fetchCategories = async () => {
    try {
      const response = await fetch(`${API_URL}/api/getcategory`);
      if (response.ok) {
        const data = await response.json();
        setCategories(data);
      }
    } catch (error) {
      console.error("카테고리 API 호출 오류:", error);
    }
  };

  // 📥 주소 데이터 가져오기
  const fetchAddresses = async () => {
    try {
      const response = await fetch(`${API_URL}/api/getaddress`);
      if (response.ok) {
        const data = await response.json();
        console.log("주소 데이터:", data);
        setAddresses(data);
      }
    } catch (error) {
      console.error("주소 API 호출 오류:", error);
    }
  };

  // ✅ 페이지 로드 및 필터 변경 시 API 호출
  useEffect(() => {
    console.log("📤 Sending request to API with:", {
      selectedCategory,
      selectedCity,
      selectedDistrict,
    });
    fetchPlaces(page === 0); // page가 0이면 데이터를 초기화
  }, [page, selectedCategory, selectedCity, selectedDistrict]);

  // ✅ 초기 데이터 (카테고리, 주소) 로드
  useEffect(() => {
    fetchCategories();
    fetchAddresses();
  }, []);

  // ✅ 필터 변경 시 페이지 및 데이터 초기화
  const handleFilterChange = () => {
    setPage(0);
    setPlaces([]);
  };

  // ✅ 검색 버튼 클릭 이벤트
  const handleSearch = () => {
    setSelectedCategory("");
    setSelectedCity("");
    setSelectedDistrict("");
    handleFilterChange(); // 페이지 및 데이터 초기화
    fetchPlaces(page === 0); // page가 0이면 데이터를 초기화
  };

  const subwayLineColors  = {
      "1호선": "#0052A4",
      "2호선": "#00A84D",
      "3호선": "#EF7C1C",
      "4호선": "#00A5DE",
      "5호선": "#996CAC",
      "6호선": "#CD7C2F",
      "7호선": "#747F00",
      "8호선": "#E6186C",
      "9호선": "#BDB092",
      "경의중앙선": "#77C4A3",
      "수인분당선": "#F5A200",
      "신분당선": "#D4003B",
      "공항철도": "#0090D2",
      "김포골드라인": "#A17800",
      "서해선": "#81A914",
      "의정부경전철": "#FDA600",
      "용인경전철": "#509F22",
      "우이신설선": "#B0CE18",
      "인천1호선": "#7CA8D5",
      "인천2호선": "#ED8B00",
      "대구1호선": "#D93F5C",
      "대구2호선": "#00AA80",
      "대구3호선": "#FFB100",
      "대전1호선": "#007448",
      "광주1호선": "#009088",
      "SRT": "#5A2149",
      "수도권 광역급행철도": "#9A6292",
  }

  
  const getSubwayInfo = (subwayData) => {
    if (!subwayData) return [];
  
    return subwayData.split("],[").map((item) => {
      const cleanedItem = item.replace(/\[|\]/g, ""); // 대괄호 제거
      const [station, line, distance] = cleanedItem.split(",");
      return { station, line, distance };
    });
  };

  return (
    <div style={{ maxWidth: "600px", margin: "50px auto", textAlign: "center" }}>

      {/* 🔍 검색 입력창 및 버튼 추가 */}
      <div style={{ marginBottom: "20px" }}>
        <input
          type="text"
          placeholder="상호명을 입력하세요"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          style={{
            padding: "8px",
            width: "70%",
            borderRadius: "4px",
            border: "1px solid #ddd",
            marginRight: "10px",
          }}
        />
        <button
          onClick={handleSearch}
          style={{
            padding: "8px 15px",
            borderRadius: "4px",
            border: "none",
            backgroundColor: "#2196F3",
            color: "#fff",
            cursor: "pointer",
          }}
        >
          검색
        </button>
      </div>

      {/* 🚇 지하철역 검색 입력창 및 버튼 */}
      <div style={{ marginBottom: "20px" }}>
        <input
          type="text"
          placeholder="지하철역을 입력하세요"
          value={subwaySearchTerm}
          onChange={(e) => setSubwaySearchTerm(e.target.value)}
          style={{
            padding: "8px",
            width: "70%",
            borderRadius: "4px",
            border: "1px solid #ddd",
            marginRight: "10px",
          }}
        />
        <button
          onClick={handleSearch}
          style={{
            padding: "8px 15px",
            borderRadius: "4px",
            border: "none",
            backgroundColor: "#2196F3",
            color: "#fff",
            cursor: "pointer",
          }}
        >
          검색
        </button>
      </div>

      <div style={{ marginBottom: "20px" }}>
        <select
          value={selectedCategory}
          onChange={(e) => {
            setSelectedCategory(e.target.value);
            handleFilterChange();
          }}
          style={{ padding: "8px", margin: "5px", borderRadius: "4px" }}
        >
          <option value="">카테고리 선택</option>
          {categories.map((category) => (
            <option key={category.id} value={category.name}>
              {category.name}
            </option>
          ))}
        </select>

        <select
          value={selectedCity}
          onChange={(e) => {
            setSelectedCity(e.target.value);
            setSelectedDistrict("");
            handleFilterChange();
          }}
          style={{ padding: "8px", margin: "5px", borderRadius: "4px" }}
        >
          <option value="">시/도 전체</option>
          {[...new Set(addresses.map((addr) => addr.region))].map((region) => (
            <option key={region} value={region}>
              {region}
            </option>
          ))}
        </select>

        <select
          value={selectedDistrict}
          onChange={(e) => {
            setSelectedDistrict(e.target.value);
            handleFilterChange();
          }}
          style={{ padding: "8px", margin: "5px", borderRadius: "4px" }}
        >
          <option value="">시/군/구 전체</option>
          {addresses
            .filter((addr) => addr.region === selectedCity)
            .map((addr) => (
              <option key={addr.id} value={addr.subregion}>
                {addr.subregion}
              </option>
            ))}
        </select>
      </div>

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
            <h2 style={{ fontSize: "20px", fontWeight: "bold", color: "#333" }}>{place.title}</h2>
            <p style={{ margin: "8px 0", color: "#555" }}>{place.address}</p>
            <p style={{ margin: "8px 0", color: "#444" }}>
              콜키지 가능: {place.corkageAvailable ? "가능" : "불가능"}
            </p>
            <p style={{ margin: "8px 0", color: "#444" }}>
              콜키지 비용: {place.corkageAvailable ? (place.freeCorkage ? "무료" : "유료") : "콜키지 불가능"}
            </p>

            {/* ✅ 지하철 정보 추가 (색상 적용) */}
            {place.nearbySubways && place.nearbySubways.length > 0 && (
              <div style={{ margin: "8px 0", color: "#444" }}>
                <p style={{ fontWeight: "bold" }}>🚇 가까운 지하철</p>
                <ul style={{ listStyleType: "none", padding: 0 }}>
                  {getSubwayInfo(place.nearbySubways).slice(0,3).map((subway, index) => (
                    <li key={index} style={{ marginBottom: "5px" }}>
                      <span style={{ fontWeight: "bold" }}>{subway.station}</span> - 
                      <span
                        style={{
                          backgroundColor: subwayLineColors[subway.line] || "#666",
                          color: "#fff",
                          padding: "3px 8px",
                          borderRadius: "4px",
                          marginLeft: "5px",
                        }}
                      >
                        {subway.line}
                      </span>
                      <span style={{ marginLeft: "5px", color: "#888" }}>{subway.distance}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}  

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
          onClick={() => setPage((prevPage) => prevPage + 1)}
          style={{
            padding: "10px 20px",
            marginTop: "20px",
            border: "none",
            borderRadius: "4px",
            backgroundColor: "#4CAF50",
            color: "#fff",
            cursor: "pointer",
            transition: "background 0.3s",
          }}
        >
          더보기
        </button>
      )}

      {!hasMore && <p>모든 데이터를 불러왔습니다.</p>}
    </div>
  );
}
