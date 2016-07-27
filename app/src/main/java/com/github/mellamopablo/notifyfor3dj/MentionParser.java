package com.github.mellamopablo.notifyfor3dj;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionParser {

    //The "garbage" html that we need to remove from the response
    String garbage = "<div class='barra36'><div class='txt nw oh'>Foros</div></div><div class='mar_rl2 mar_t6'><div class='sandra oh'><div class='fl'><ul class='pl0'><span class='izq'></span><li ><a href='/comunidad.php?zona=info_foro_temas' onclick=\"lc('#listado_msg', 'mis_foros', 'get_info=comu_info_foro_main&zona=info_foro_temas'); return false;\" class=' set_url'><span><b >Tus temas</b></span></a></li><li ><a href='/comunidad.php?zona=info_foro_mensajes' onclick=\"lc('#listado_msg', 'mis_foros', 'get_info=comu_info_foro_main&zona=info_foro_mensajes'); return false;\" class=' set_url'><span><b >Tus mensajes</b></span></a></li><li ><a href='/comunidad.php?zona=info_foro_menciones' onclick=\"lc('#listado_msg', 'mis_foros', 'get_info=comu_info_foro_main&zona=info_foro_menciones'); return false;\" class='on set_url'><span><b >Menciones</b></span></a></li><li ><a href='/comunidad.php?zona=info_foro_votos' onclick=\"lc('#listado_msg', 'mis_foros', 'get_info=comu_info_foro_main&zona=info_foro_votos'); return false;\" class=' set_url'><span><b >Votos recientes</b></span></a></li><li style='margin-right:0px'><a href='/comunidad.php?zona=foro_temas_favoritos' onclick=\"lc('#listado_msg', 'mis_foros', 'get_info=comu_info_foro_main&zona=foro_temas_favoritos'); return false;\" class=' set_url'><span><b >Favoritos</b></span></a></li></ul></div><span class='der'></span></div></div><div class='mar_rl5 mar_t8 mar_b4'><div class='caja_gris_opc_p6 mar_tb6'><div class='fr s14 c3 b pad_tb6'></div><div class='pad_6 s14 c2'>Usuarios que te han mencionado</div></div><div class='he8'></div>";
    private String html;


    public MentionParser(String html) {
        this.html = html;
    }

    public List<Mention> parse() throws Exception {
        String newHtml = html.replace(garbage, "");

        //By matching the first line we get rid of all the useless javascript sent to us
        Pattern matchFirstLine = Pattern.compile("(.*)");
        Matcher matcher = matchFirstLine.matcher(newHtml);
        if (matcher.find()) {
            newHtml = matcher.group(0);
        } else {
            throw new Exception("Couldn't match the first line of the returned html");
        }

        Document doc = Jsoup.parse(newHtml);
        Elements mentions = doc.select("tr[bgcolor]"); //Only mentions have bgcolor

        List<Mention> list = new ArrayList<>();

        for (Element e : mentions) {
            String username = e.child(1).child(0).text();
            String avatar_url = e.child(0).child(0).child(0).attr("src");
            String profile_url = "http://www.3djuegos.com" + e.child(1).child(0).attr("href");
            String msg_url = e.child(2).child(0).child(0).attr("href");
            String thread = e.child(2).child(0).child(0).text();
            String thatLongAgo = e.child(1).child(2).text();
            int timestamp = Integer.parseInt(e.child(1).child(2).attr("data-time"));

            Pattern getId = Pattern.compile("elimina_mencion\\(([0-9]+),'foro'\\);");
            Matcher idMatcher = getId.matcher(e.child(3).child(0).attr("onClick"));
            int id;
            if (idMatcher.find()) {
                id = Integer.parseInt(idMatcher.group(1));
            } else {
                throw new Exception("Couldn't get the ID of a mention");
            }

            Mention m = new Mention(username, avatar_url, profile_url, msg_url, thread,
                    thatLongAgo, timestamp, id);

            list.add(m);
        }

        return list;
    }
}
