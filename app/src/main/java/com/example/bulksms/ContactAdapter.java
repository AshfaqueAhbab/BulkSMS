package com.example.bulksms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    public interface Listener {
        void onSelectionChanged(int count);
        void onLongPress(int position);
    }

    // allContacts is the same reference as MainActivity.contacts — mutations there
    // are immediately visible here; call refreshFilter() after any mutation.
    private final List<Contact> allContacts;
    private List<Contact> filteredContacts;
    private final Set<Contact> selected = new HashSet<>(); // object-identity based
    private final Listener listener;
    private String currentFilter = "";

    ContactAdapter(List<Contact> allContacts, Listener listener) {
        this.allContacts = allContacts;
        this.filteredContacts = new ArrayList<>(allContacts);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact c = filteredContacts.get(position);
        holder.name.setText(c.getName());
        holder.phone.setText(c.getPhone());
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(selected.contains(c));

        View.OnClickListener toggle = v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) toggleSelection(pos);
        };
        holder.itemView.setOnClickListener(toggle);
        holder.checkbox.setOnClickListener(toggle);

        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onLongPress(pos);
            return true;
        });
    }

    private void toggleSelection(int position) {
        Contact c = filteredContacts.get(position);
        if (!selected.remove(c)) {
            selected.add(c);
        }
        notifyItemChanged(position);
        listener.onSelectionChanged(selected.size());
    }

    @Override
    public int getItemCount() { return filteredContacts.size(); }

    public Contact getItem(int position) {
        return filteredContacts.get(position);
    }

    /** Rebuild the displayed list from allContacts, re-applying the current filter. */
    public void refreshFilter() {
        filter(currentFilter);
    }

    public void filter(String query) {
        currentFilter = query == null ? "" : query.trim().toLowerCase();
        if (currentFilter.isEmpty()) {
            filteredContacts = new ArrayList<>(allContacts);
        } else {
            filteredContacts = new ArrayList<>();
            for (Contact c : allContacts) {
                if (c.getName().toLowerCase().contains(currentFilter)
                        || c.getPhone().contains(currentFilter)) {
                    filteredContacts.add(c);
                }
            }
        }
        notifyDataSetChanged();
        listener.onSelectionChanged(selected.size());
    }

    /** Select all contacts currently visible (matching the active filter). */
    public void selectAllFiltered() {
        selected.addAll(filteredContacts);
        notifyDataSetChanged();
        listener.onSelectionChanged(selected.size());
    }

    public int getSelectedCount() { return selected.size(); }

    public List<Contact> getSelectedContacts() {
        return new ArrayList<>(selected);
    }

    public void clearSelection() {
        selected.clear();
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkbox;
        final TextView name, phone;

        ViewHolder(View v) {
            super(v);
            checkbox = v.findViewById(R.id.checkboxSelect);
            name = v.findViewById(R.id.textName);
            phone = v.findViewById(R.id.textPhone);
        }
    }
}
