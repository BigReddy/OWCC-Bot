package de.tu_darmstadt.informatik.robert_jakobi.tb.util;

/**
 * Interface to outsource big chunks of text.
 *
 * @author Robert Jakobi
 * @since 22/10/18
 * @version 1.0
 *
 */
public interface MessageContend {
    String MARKET_HELP = "**!sr** [number]\t*Set your SR*" //
            + "\n**!role** [Tank|Support|DPS|Flex]\t*Sets your prefered role*" //
            + "\n**!search** <-sr [number]> <-role [Tank|Support|DPS|Flex]> <-range [number]>\t*Search for fitting player*";
    String MARKET_SEARCH_HELP = "!search" + "\n\t-sr [number]\t\t*SR searching for (default: 0)*"
            + "\n\t-role [Tank|Support|DPS|Flex]\t*Role searching for (default: any)*"
            + "\n\t-range [number]\t\t*SR range within looking for (default: 300 or 5000 (if SR = 0)*"
            + "\n\t-notify true\t\t*Notifies you, if player with given parameters registers*"
            + "\n\t-remove [all|role]\t\t*Removes all registered requests or all with given role*";
}
