package chiralsoftware.mwtj.controllers;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import static com.google.common.collect.Ordering.natural;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import static java.net.http.HttpClient.Redirect.NEVER;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import java.util.Optional;
import java.util.logging.Logger;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility methods to remove trackers from URLs.
 * Todo: maybe add a feature to unwrap redirect URLs like: https://t.co/aFsmOoCU9x . This URL just returns a 301 with a Location: header
 */
final class DerUntracker {

    private static final Logger LOG = Logger.getLogger(DerUntracker.class.getName());
    
    static final ImmutableSortedMap<String,String> prefixAndParameters =
            new ImmutableSortedMap.Builder<String,String>(natural()).
                    put("https://www.google.com/url", "url").
                    put("https://go.redirectingat.com/", "url"). 
                    
                    // these could be combined into a regex
                    put("https://adclick.g.doubleclick.net/aclk", "adurl").
                    put("https://adclick.g.doubleclick.net/pcs/click", "adurl"). // doubleclick ads - test this out
                    put("https://googleads.g.doubleclick.net/aclk", "adurl"). // doubleclick ads - test this out
                    
                    // https://www.avantlink.com/click.php?ctc=gearreviews%2Fbest-winter-gloves_amcid-rbANirAFmeN6ArliYeVAF&merchant_id=b5770911-39dc-46ac-ba0f-b49dbb30c5c7&tt=cl&url=https%3A%2F%2Fwww.backcountry.com%2Fthe-north-face-etip-denali-gloves-mens&website_id=2ea4ea95-bcd0-4bf8-a848-64c4dd59a76d
                    put("https://www.avantlink.com/click.php", "url").
                    put("https://target.georiot.com/Proxy.ashx", "GR_URL"). // GeniusLink, https://geniuslink.com 
                    put("https://www.youtube.com/redirect", "q").
                    put("https://l.facebook.com/l.php", "u"). // fb redirect links
                    // like this: https://analytics.oemsecrets.com/main.php?p=EM120KGLAA-M22-SGADA&m=Quectel&q=0&n=Digi-Key&table=api&media=buynow&source=quectel&event_link=https%3A%2F%2Fwww.digikey.com%2Fen%2Fproducts%2Fdetail%2Fquectel%2FEM120KGLAA-M22-SGADA%2F21272521
                    put("https://analytics.oemsecrets.com/", "event_link").
                    build();
    
    private static final ImmutableSortedSet<String> queryParamsToDelete =
            new ImmutableSortedSet.Builder<String>(natural()).
            add("ref_src").
            add("ref_url").
            add("fbclid").
            add("igshid").
                    add("igsh").
            add("gclid").
            add("gad").
            add("taid").
            add("cid").
            add("si"). // this shows up on youtube links
//            add("").
                    build();
    
        
    static String removeTrackers(String urlString) {
        if(! urlString.startsWith("https://")) return urlString;
        
        // FIXME: this is overly aggressive because it chops off everything after it has detected
        // any of the tracker query parameters. Usually they are stuck to the end of the URL but
        // it might be safer to actually parse the URL and remove unwanted query parameters correctly and then
        // recreate the URL
        for(String s : queryParamsToDelete) {
            urlString = urlString.replaceFirst("\\?" + s + "=.+$", "");
            urlString = urlString.replaceFirst("&" + s + "=.+$", "");
        }

            // look for utm_ entries like this:
        // https://www.example.com/events/2952617?utm_medium=email&utm_source=sendgrid&utm_campaign=event_announce_en
        urlString = urlString.replaceFirst("\\?utm_[a-z_]+=.+$", "");

        if(urlString.startsWith("https://www.amazon.com/")) {
            // everything after /ref= isn't needed
            urlString = urlString.replaceFirst("/ref=.+$", "");
            // amazon links never require parameters either
            urlString = urlString.replaceFirst("\\?.+$", "");
        }

        return urlString;
    }
    
    
    /** Take a string and remove redirect-based tracker from it. It returns a fixed URL if it detected a redirect,
     null otherwise. If it returns non-null, that's a final result to return. Otherwise, continue 
     with the tracker removal.
     * Example: https://go.redirectingat.com/?id=31959X896062&xs=1&url=https%3A%2F%2Fwww.matchesfashion.com%2Fus%2Fproducts%2FStefan-Cooke-Martlett-stud-embellished-leather-Derby-shoes-1494292&sref=https%3A%2F%2Fwww.gearpatrol.com%2Fstyle%2Fshoes-boots%2Fa42168034%2Fbest-new-boots-shoes-sneakers-2022%2F
     */
    static String removeRedirect(String urlString)  {
        if(urlString == null) return null;
        for(String s : prefixAndParameters.keySet()) {
            if(startsWithIgnoreCase(urlString,s)) {
                final String paramName = prefixAndParameters.get(s);
                final MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(urlString).build().getQueryParams();
                if(params.containsKey(paramName)) {
                    String urlResultString = params.getFirst(paramName);
                    if(isBlank(urlResultString)) return null;
                    try {
                        urlResultString = URLDecoder.decode(urlResultString, UTF_8.name());
                    } catch(UnsupportedEncodingException uee) { 
                        return null;
                    }
                    return removeTrackers(urlResultString);
                }
            }
        }
        // it wasn't one of the known redirecters 
        return null;
    }

    private static final HttpClient httpClient
                = HttpClient.newBuilder().followRedirects(NEVER).build();
    
    private static final ImmutableSortedSet<String> remoteRediectors
            = new ImmutableSortedSet.Builder<String>(natural()).
                    add("https://t.co/").
                    add("https://bit.ly/").
                    add("https://amzn.to/").
                    add("https://a.co/"). // example: https://a.co/d/aEfICVn
                    add("https://gofund.me/"). // these are like: https://gofund.me/a680f986
                    add("https://cna.st/affiliate-link/"). // example: https://cna.st/affiliate-link/5Ct....
                    build();
    
    /** Handle redirectors: t.co, bit.ly. This returns NULL if the URL fails for any reason, such as it's not a redirect URL, 
     * not found, invalid, etc */
    static String remoteRedirect(String urlString) throws URISyntaxException, MalformedURLException, IOException, InterruptedException {
        if (urlString.length() > 300)
            return null; // long strings are not redirections
        if (! remoteRediectors.stream().anyMatch(rd -> startsWithIgnoreCase(urlString, rd))) 
            return null;
        final HttpRequest request = HttpRequest.
                newBuilder(new URI(urlString)).
                timeout(ofSeconds(2)). // sometimes gofundme is slow
                GET().
                build();
        final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        if (! (response.statusCode() == 301 || response.statusCode() == 302)) // gofundme and cna.st returns 302 (temporary), the others return 301 (permanent)
            return null;
        final Optional<String> locationHeader = response.headers().firstValue("location");
        return locationHeader.orElse(null);
    }
    
}
