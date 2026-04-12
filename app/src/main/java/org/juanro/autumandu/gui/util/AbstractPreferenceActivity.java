/*
 * Copyright 2017 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.juanro.autumandu.gui.util;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import org.juanro.autumandu.R;

/**
 * Actividad base moderna para pantallas de preferencias.
 */
public abstract class AbstractPreferenceActivity extends AppCompatActivity {

    /**
     * Interfaz para fragmentos que proporcionan un menú.
     * Extiende MenuProvider para usar las APIs modernas de AndroidX y evitar métodos obsoletos.
     */
    public interface OptionsMenuListener extends MenuProvider {
    }

    private Toolbar mToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference_base);

        mToolbar = findViewById(R.id.action_bar);
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getTitleResourceId());
        }

        mToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Reemplazo moderno para la gestión de menús en fragmentos
        getSupportFragmentManager().addFragmentOnAttachListener((fragmentManager, fragment) -> {
            if (fragment instanceof OptionsMenuListener menuListener) {
                // Usamos el ciclo de vida del fragmento en lugar del de su vista
                // para evitar IllegalStateException cuando la vista aún no existe.
                addMenuProvider(menuListener, fragment, Lifecycle.State.RESUMED);
            }
        });
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        if (mToolbar != null) {
            mToolbar.setTitle(title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract int getTitleResourceId();
}
