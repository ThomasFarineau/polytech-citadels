package fr.unice.polytech.citadels.strategies;

import fr.unice.polytech.citadels.cards.characters.Character;
import fr.unice.polytech.citadels.cards.characters.CharacterType;
import fr.unice.polytech.citadels.cards.districts.District;
import fr.unice.polytech.citadels.cards.districts.DistrictColor;
import fr.unice.polytech.citadels.players.Player;
import fr.unice.polytech.citadels.players.PlayerAction;

import java.util.*;
import java.util.stream.Collectors;

public class FiveColorsStrategy extends Strategy {

    public FiveColorsStrategy(Player player) {
        super(player);
    }

    @Override
    public void switchStrategy() {
        if (player.getDistrictsBoard().containsColorSet()) {
            try {
                player.setStrategy(player.getScore() < 26 ? ExpGoldStrategy.class : CheapDistrictStrategy.class); // Ici, le joueur change de stratégie en fonction de son score pour choisir la plus adaptée
            } catch (Exception e) {
                e.printStackTrace();
            }
            printStrategyChange();
        }
    }

    @Override
    public Character chooseCharacter(Character[] selectable, Character[] playable, List<Player> playerList) {
        Character chosen;
        if (player.getDistrictsBoard().containsColorSet()) {
            boolean warlord = Arrays.stream(playable).anyMatch(character -> character.getType().equals(CharacterType.WARLORD));
            if (warlord) {
                // S'il a posé toutes les couleurs il choisira évêque si la carte condottière est en jeu
                chosen = containsCharacter(selectable, CharacterType.BISHOP);
                if (chosen != null) return chosen;

                // Si le condottière et en jeu et qu'il n'y a pas l'évêque il prend l'assassin pr tuer le condottière
                chosen = containsCharacter(selectable, CharacterType.ASSASSIN);
                if (chosen != null) return chosen;
            }
        } else {
            boolean colors;
            Set<DistrictColor> colorSetBoard = player.getDistrictsBoard().stream().map(District::getColor).collect(Collectors.toSet());
            Set<DistrictColor> colorSetHand = player.getDistrictsHand().stream().map(District::getColor).collect(Collectors.toSet());
            colors = colorSetBoard.containsAll(colorSetHand);

            // S'il n'a que des cartes de couleur qu'il a déjà posé alors il choisit magicien
            if (colors && player.getDistrictsHand().size() != 0) {
                chosen = containsCharacter(selectable, CharacterType.MAGICIAN);
                if (chosen != null) return chosen;
            }

            // Si il n'a pas de carte alors il choisit architecte
            if (player.getDistrictsHand().size() == 0) {
                chosen = containsCharacter(selectable, CharacterType.ARCHITECT);
                if (chosen != null) return chosen;
            }

            // S'il a des cartes, mais pas d'or il choisit marchant
            if (player.getDistrictsHand().size() > 0 && player.getGold() < 2) {
                chosen = containsCharacter(selectable, CharacterType.MERCHANT);
                if (chosen != null) return chosen;
            }
        }

        // Sinon il prend un perso random
        Random random = new Random();
        // Choix d'un personnage aléatoirement parmi se proposer
        return selectable[random.nextInt(selectable.length)];
    }

    @Override
    public PlayerAction chooseGoldOrDistrict() {
        // Si il a des batiments a placé d'une couleur différente de ce qui déjà été placé il prend les golds
        boolean takeGold = false;
        for (District hand : player.getDistrictsHand())
            if (player.getDistrictsBoard().getByColor(hand.getColor()).length == 0) takeGold = true;
        return takeGold ? PlayerAction.PICK_GOLDS : PlayerAction.PICK_DISTRICTS;
    }

    @Override
    public District chooseDistrict(District... districts) {
        List<District> toReturn = new ArrayList<>();
        Random random = new Random();
        int weight = -1;
        for (District district : districts) {
            int n = player.getDistrictsBoard().getByColor(district.getColor()).length + player.getDistrictsHand().getByColor(district.getColor()).length;
            if (weight == -1 || weight > n) weight = n;
        }
        for (District district : districts) {
            int n = player.getDistrictsBoard().getByColor(district.getColor()).length + player.getDistrictsHand().getByColor(district.getColor()).length;
            if (n == weight) toReturn.add(district);
        }
        return toReturn.get(random.nextInt(toReturn.size()));
    }

    @Override
    public boolean placeDistrict() {
        // Il place en priorité les quartiers dont il na pas encore posé de couleur
        if (!player.getDistrictsBoard().containsColorSet()) {
            Set<DistrictColor> colorSet = player.getDistrictsBoard().stream().map(District::getColor).collect(Collectors.toSet());
            List<District> districtsPlacable = player.getDistrictsHand().stream().filter(district -> !colorSet.contains(district.getColor())).sorted((o1, o2) -> o2.getPrice() - o1.getPrice()).collect(Collectors.toList());
            // On prend la carte du millieu de la liste et si on ne peux pas la posé alors on la supprime de placable
            District chosen = null;
            while (chosen == null && districtsPlacable.size() > 0) {
                District d = districtsPlacable.get((districtsPlacable.size() / 2));
                if (player.getGold() >= d.getPrice()) chosen = d;
                districtsPlacable.remove(d);
            }
            if (chosen != null) {
                player.placeDistrict(chosen);
                return true;
            }
        } else {
            District d;
            Random random = new Random();
            int counter = 0;
            boolean hasPlaced;
            if (super.player.getDistrictsHand().size() > 0) {
                do {
                    counter++;
                    d = super.player.getDistrictsHand().get(random.nextInt(super.player.getDistrictsHand().size()));
                } while (!(hasPlaced = super.player.placeDistrict(d)) && counter != super.player.getDistrictsHand().size());
                return hasPlaced;
            }
        }
        return false;
    }

    public District whatDistrictToChoose() {
        if (!player.getDistrictsBoard().containsColorSet()) {
            Set<DistrictColor> colorSet = player.getDistrictsBoard().stream().map(District::getColor).collect(Collectors.toSet());
            List<District> districtsPlacable = player.getDistrictsHand().stream().filter(district -> !colorSet.contains(district.getColor())).sorted((o1, o2) -> o2.getPrice() - o1.getPrice()).collect(Collectors.toList());
            // On prend la carte du milieu de la liste et si on ne peut pas la poser alors on la supprime de plaçable
            District chosen = null;
            while (chosen == null && districtsPlacable.size() > 0) {
                District d = districtsPlacable.get((districtsPlacable.size() / 2));
                if (player.getGold() >= d.getPrice()) chosen = d;
                districtsPlacable.remove(d);
            }
            return chosen;
        } else {
            District d;
            Random random = new Random();
            int counter = 0;
            if (player.getDistrictsHand().size() > 0) {
                do {
                    counter++;
                    d = player.getDistrictsHand().get(random.nextInt(player.getDistrictsHand().size()));
                } while (!player.placeDistrict(d) && counter != player.getDistrictsHand().size());
                return d;
            }
        }
        return null;
    }

    @Override
    public PlayerAction whatToDo() {
        // On regarde son quartier le plus cher
        // Si le joueur peut utiliser son ability alors il l'a joue
        if (player.canUseAbility()) return PlayerAction.USE_ABILITY;
        // Si on peut le poser alors on choisit de poser son quartier
        District d = whatDistrictToChoose();
        if (d != null && d.getPrice() <= getPlayer().getGold() && getPlayer().canPlaceDistrict())
            return PlayerAction.PLACE_DISTRICT;
        // Si on peut récupérer les po avec les districts alors on le fait
        if (getPlayer().canCollectGoldsFromDistrict()) return PlayerAction.COLLECT_GOLDS_FROM_DISTRICTS;
        return PlayerAction.FINISH;
    }
}
