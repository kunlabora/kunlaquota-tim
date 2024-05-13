package be.kunlabora.crafters.kunlaquota.jitterted.lobby

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute

@Controller
class LobbyController {
    private val gameViews: List<GameView> = java.util.List.of(GameView("Ted's Room", 42))

    class LobbyModel {
        var gameViews: List<GameView> = ArrayList()
    }


    @GetMapping("/lobby")
    fun showLobby(@ModelAttribute lobbyModel: LobbyModel): String {
        lobbyModel.gameViews = gameViews
        return "showLobby"
    }

    @JvmRecord
    data class GameView(val name: String, val playerCount: Int)
}
