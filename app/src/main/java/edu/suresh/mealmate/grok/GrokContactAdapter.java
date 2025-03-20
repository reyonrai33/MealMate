package edu.suresh.mealmate.grok;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.suresh.mealmate.R;

public class GrokContactAdapter extends RecyclerView.Adapter<GrokContactAdapter.ViewHolder> {
    private List<Contact> contacts;
    private Set<Contact> selectedContacts = new HashSet<>();

    public GrokContactAdapter(List<Contact> contacts) {
        this.contacts = contacts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grok_item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.textView.setText(contact.name + "\n" + contact.number);
        holder.checkBox.setChecked(selectedContacts.contains(contact));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedContacts.add(contact);
            } else {
                selectedContacts.remove(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public List<Contact> getSelectedContacts() {
        return new ArrayList<>(selectedContacts);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCheckBox checkBox;
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.contactCheckBox);
            textView = itemView.findViewById(R.id.contactTextView);
        }
    }
}