import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import SimpleTextDisplay from "./SimpleTextDisplay";
import PlaceListPage from "./PlaceListPage";

export default function CorkageApp() {
    return (
      <Router>
        <div style={{ maxWidth: "600px", margin: "50px auto", textAlign: "center" }}>
          <h1>콜키지 정보 메인 페이지</h1>
          <div>
            <Link to="/places">
              <button style={buttonStyle}>콜키지 리스트 보기</button>
            </Link>
            <Link to="/search">
              <button style={buttonStyle}>콜키지 검색 및 등록하기</button>
            </Link>
          </div>
  
          <Routes>
            <Route path="/places" element={<PlaceListPage />} />
            <Route path="/search" element={<SimpleTextDisplay />} />
          </Routes>
        </div>
      </Router>
    );
  }
  
  const buttonStyle = {
    padding: "10px 20px",
    margin: "10px",
    border: "none",
    borderRadius: "4px",
    backgroundColor: "#4CAF50",
    color: "#fff",
    cursor: "pointer",
  }