/*
 * Copyright 2014 Jan Kühle
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

package org.juanro.autumandu.gui.fragment;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Interface para la comunicación entre los fragmentos de lista de datos
 * y su contenedor principal (DataFragment).
 */
public interface DataListCallback {
    /**
     * Llamado cuando la vista de la lista ha sido creada.
     * @param recyclerView La instancia del RecyclerView creada.
     */
    void onViewCreated(@NonNull RecyclerView recyclerView);

    /**
     * Llamado cuando se selecciona un elemento de la lista.
     * @param edit El tipo de edición/detalle (definido en DataDetailActivity).
     * @param id El ID único del elemento en la base de datos (Room).
     */
    void onItemSelected(int edit, long id);

    /**
     * Llamado cuando no hay ningún elemento seleccionado (ej. al cerrar un detalle).
     */
    void onItemUnselected();
}
