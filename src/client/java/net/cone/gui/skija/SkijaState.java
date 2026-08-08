package net.cone.gui.skija;

import net.cone.gui.skija.ui.SearchField;
import net.cone.gui.skija.ui.Widget;

import java.util.List;

final class SkijaState {
    List<SkijaModel.Category> categories = SkijaModel.build();

    int selectedCat = 0;

    String expandedId;

    String query = "";
    float scroll;

    final SearchField search = new SearchField(q -> {
        query = q;
        scroll = 0;
    });

    Widget dragging;
    float dragDens = 1;

    Widget hover;
    long hoverSince;

    void rebuild() {
        blurAll();
        categories = SkijaModel.build();
        if (selectedCat >= categories.size()) selectedCat = categories.size() - 1;
    }

    void blurAll() {
        for (SkijaModel.Category c : categories) {
            for (SkijaModel.Module m : c.modules) m.body.blur();
        }
        dragging = null;
    }

    boolean dashboard() {
        return query.isBlank() && selectedCat == 0;
    }

    List<SkijaModel.Module> visibleModules() {
        if (!query.isBlank()) return SkijaModel.search(categories, query);
        return categories.get(selectedCat).modules;
    }

    String listTitle() {
        if (!query.isBlank()) return "Search";
        return categories.get(selectedCat).name;
    }

    boolean anyTyping() {
        if (search.focused()) return true;
        for (SkijaModel.Module m : visibleModules()) {
            if (m.body.typing() || m.body.listening()) return true;
        }
        return false;
    }

    void selectCategory(int i) {
        if (i == selectedCat && query.isBlank()) return;
        selectedCat = i;
        query = "";
        search.clear();
        scroll = 0;
        expandedId = null;
        rebuild();
    }
}
