package com.parkminje.pingpong.operation;

import com.parkminje.pingpong.entry.Entry;
import com.parkminje.pingpong.live.TournamentBoard;
import com.parkminje.pingpong.live.TournamentBoardService;
import com.parkminje.pingpong.match.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 엑셀에서 바로 열리는 CSV (UTF-8 BOM) */
@Controller
@RequiredArgsConstructor
@RequestMapping("/manage/tournaments/{id}/export")
public class ExportController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TournamentBoardService boardService;

    @GetMapping("/entries.csv")
    public ResponseEntity<byte[]> entries(@PathVariable Long id) {
        TournamentBoard board = boardService.load(id);
        StringBuilder sb = new StringBuilder();
        row(sb, "번호", "선수", "파트너", "클럽", "부수", "레이팅", "상태", "체크인", "입금", "시드", "조", "메모", "신청일시");
        int no = 1;
        for (Entry e : board.entries()) {
            row(sb, String.valueOf(no++), e.getPlayer().getName(),
                    e.getPartner() == null ? "" : e.getPartner().getName(),
                    e.getSubLabel(), e.getPlayer().getDivisionLabel(), String.valueOf(e.getStrength()),
                    e.getStatus().getLabel(), e.isCheckedIn() ? "O" : "", e.isPaid() ? "O" : "",
                    e.getSeed() == null ? "" : String.valueOf(e.getSeed()),
                    e.getGroup() == null ? "" : e.getGroup().getName(),
                    e.getMemo() == null ? "" : e.getMemo(),
                    e.getCreatedAt() == null ? "" : e.getCreatedAt().format(TIME));
        }
        return csv(board.tournament().getTitle() + "_참가자", sb);
    }

    @GetMapping("/results.csv")
    public ResponseEntity<byte[]> results(@PathVariable Long id) {
        TournamentBoard board = boardService.load(id);
        StringBuilder sb = new StringBuilder();
        row(sb, "순서", "구분", "라운드", "선수1", "선수2", "승자", "결과", "게임 점수", "테이블", "상태", "종료시각");
        List<Match> matches = board.matches();
        for (Match m : matches) {
            row(sb, String.valueOf(m.getPlayOrder()), m.getStage().getLabel(), m.getRoundLabel(),
                    name(m.getEntry1()), name(m.getEntry2()), name(m.getWinner()),
                    m.getScoreLabel(), m.getGameScores() == null ? "" : m.getGameScores().replace(",", " / "),
                    m.getTableNumber() == null ? "" : String.valueOf(m.getTableNumber()),
                    m.getStatus().getLabel(), m.getFinishedAt() == null ? "" : m.getFinishedAt().format(TIME));
        }
        return csv(board.tournament().getTitle() + "_경기결과", sb);
    }

    private static String name(Entry e) {
        return e == null ? "" : e.getDisplayName();
    }

    private static void row(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells[i]));
        }
        sb.append("\r\n");
    }

    /** 쉼표·따옴표·줄바꿈 이스케이프 + 수식 주입(=,+,-,@) 방지 */
    static String escape(String v) {
        if (v == null) {
            return "";
        }
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static ResponseEntity<byte[]> csv(String filename, StringBuilder body) {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = body.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(content, 0, out, bom.length, content.length);
        String safe = filename.replaceAll("[\\\\/:*?\"<>|]", "_") + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(safe, StandardCharsets.UTF_8).build().toString())
                .body(out);
    }
}
