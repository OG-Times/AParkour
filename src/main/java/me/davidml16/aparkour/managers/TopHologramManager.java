package me.davidml16.aparkour.managers;

import me.davidml16.aparkour.Main;
import me.davidml16.aparkour.data.LeaderboardEntry;
import me.davidml16.aparkour.data.Parkour;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.line.TextHologramLine;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class TopHologramManager {

    private final Map<String, Hologram> holoHeader;
    private final Map<String, Hologram> holoBody;
    private final Map<String, Hologram> holoFooter;  // Changed from TextHologramLine to Hologram

    private int timeLeft;
    private int reloadInterval;

    private Main main;

    public TopHologramManager(Main main, int reloadInterval) {
        this.main = main;
        this.reloadInterval = reloadInterval;
        this.holoHeader = new HashMap<>();
        this.holoBody = new HashMap<>();
        this.holoFooter = new HashMap<>();
    }

    public Map<String, Hologram> getHoloHeader() {
        return holoHeader;
    }

    public Map<String, Hologram> getHoloBody() {
        return holoBody;
    }

    public Map<String, Hologram> getHoloFooter() {
        return holoFooter;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setReloadInterval(int reloadInterval) {
        this.reloadInterval = reloadInterval;
    }

    public void restartTimeLeft() {
        this.timeLeft = reloadInterval;
    }

    public void loadTopHolograms() {
        holoBody.clear();
        holoHeader.clear();
        holoFooter.clear();
        if (main.isHologramsEnabled()) {
            for (String parkour : main.getParkourHandler().getParkours().keySet()) {
                loadTopHologram(parkour);
            }
        }
    }

    public void loadTopHologram(String id) {
        if (main.isHologramsEnabled()) {
            Parkour parkour = main.getParkourHandler().getParkours().get(id);

            main.getDatabaseHandler().getParkourBestTimes(parkour.getId(), 10).thenAccept(leaderboard -> {
                main.getLeaderboardHandler().addLeaderboard(parkour.getId(), leaderboard);

                Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                    if (parkour.getTopHologram() != null) {
                        Hologram header = HolographicDisplaysAPI.get(main).createHologram(
                                parkour.getTopHologram().clone().add(0.5D, 4.5D, 0.5D));
                        header.getLines().appendText(main.getLanguageHandler()
                                .getMessage("Holograms.Top.Header.Line1").replaceAll("%parkour%", parkour.getName()));
                        header.getLines().appendText(main.getLanguageHandler()
                                .getMessage("Holograms.Top.Header.Line2").replaceAll("%parkour%", parkour.getName()));

                        Hologram body = HolographicDisplaysAPI.get(main).createHologram(
                                parkour.getTopHologram().clone().add(0.5D, 3.75D, 0.5D));

                        Hologram footer = HolographicDisplaysAPI.get(main).createHologram(
                                parkour.getTopHologram().clone().add(0.5D, 1D, 0.5D));
                        footer.getLines().appendText(main.getLanguageHandler()
                                .getMessage("Holograms.Top.Footer.Line")
                                .replaceAll("%time%", main.getTimerManager().millisToString(main.getLanguageHandler().getMessage("Timer.Formats.HologramUpdate"), timeLeft * 1000)));

                        if(leaderboard != null) {
                            int i = 0;
                            for (LeaderboardEntry entry : leaderboard) {
                                String line = main.getLanguageHandler()
                                        .getMessage("Holograms.Top.Body.Line")
                                        .replaceAll("%player%", main.getPlayerDataHandler().getPlayerName(body.getPosition().getWorldIfLoaded(), entry.getName()))
                                        .replaceAll("%position%", Integer.toString(i + 1))
                                        .replaceAll("%time%", main.getTimerManager().millisToString(main.getLanguageHandler().getMessage("Timer.Formats.ParkourTimer"), entry.getTime()));

                                body.getLines().appendText(line);
                                i++;
                            }
                            for (int j = i; j < 10; j++) {
                                body.getLines().appendText(main.getLanguageHandler()
                                        .getMessage("Holograms.Top.Body.NoTime").replaceAll("%position%", Integer.toString(j + 1)));
                            }
                        } else {
                            for (int i = 0; i < 10; i++) {
                                body.getLines().appendText(main.getLanguageHandler()
                                        .getMessage("Holograms.Top.Body.NoTime").replaceAll("%position%", Integer.toString(i + 1)));
                            }
                        }

                        holoHeader.put(id, header);
                        holoBody.put(id, body);
                        holoFooter.put(id, footer);
                    }
                }, 20L);
            });
        }
    }

    public void reloadTopHolograms() {
        if (main.isHologramsEnabled()) {
            if (timeLeft <= 0) {
                for (Parkour parkour : main.getParkourHandler().getParkours().values()) {

                    if(parkour.getTopHologram() != null) {
                        if (holoFooter.containsKey(parkour.getId())) {
                            ((TextHologramLine) holoFooter.get(parkour.getId()).getLines().get(0))
                                    .setText(main.getLanguageHandler().getMessage("Holograms.Top.Footer.Updating"));
                        }
                    }

                    main.getDatabaseHandler().getParkourBestTimes(parkour.getId(), 10).thenAccept(leaderboard -> {
                        main.getLeaderboardHandler().addLeaderboard(parkour.getId(), leaderboard);

                        Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                            if(parkour.getTopHologram() != null) {
                                if (holoBody.containsKey(parkour.getId()) && holoFooter.containsKey(parkour.getId())) {
                                    Hologram body = holoBody.get(parkour.getId());
                                    int i = 0;
                                    for (; i < leaderboard.size(); i++) {
                                        ((TextHologramLine) body.getLines().get(i)).setText(main.getLanguageHandler()
                                                .getMessage("Holograms.Top.Body.Line").replaceAll("%position%", Integer.toString(i + 1))
                                                .replaceAll("%player%", main.getPlayerDataHandler().getPlayerName(body.getPosition().getWorldIfLoaded(), leaderboard.get(i).getName()))
                                                .replaceAll("%time%", main.getTimerManager().millisToString(main.getLanguageHandler().getMessage("Timer.Formats.ParkourTimer"), leaderboard.get(i).getTime())));
                                    }
                                    for (int j = i; j < 10; j++) {
                                        ((TextHologramLine) body.getLines().get(j)).setText(main.getLanguageHandler()
                                                .getMessage("Holograms.Top.Body.NoTime").replaceAll("%position%", Integer.toString(j + 1)));
                                    }
                                }
                            }
                        }, 20L);
                    });
                }

                restartTimeLeft();
            }
            for (String parkour : main.getParkourHandler().getParkours().keySet()) {
                if (holoFooter.containsKey(parkour)) {
                    ((TextHologramLine) holoFooter.get(parkour).getLines().get(0))
                            .setText(main.getLanguageHandler()
                                    .getMessage("Holograms.Top.Footer.Line")
                                    .replaceAll("%time%", main.getTimerManager().millisToString(main.getLanguageHandler().getMessage("Timer.Formats.HologramUpdate"), timeLeft * 1000)));
                }
            }
            timeLeft--;
        }
    }

    public void removeHologram(String id) {
        if (main.isHologramsEnabled()) {
            if (holoHeader.containsKey(id)) {
                holoHeader.get(id).delete();
                holoHeader.remove(id);
            }

            if (holoBody.containsKey(id)) {
                holoBody.get(id).delete();
                holoBody.remove(id);
            }

            if (holoFooter.containsKey(id)) {
                holoFooter.get(id).delete();
                holoFooter.remove(id);
            }
        }
    }

}