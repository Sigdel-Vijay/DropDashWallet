package com.devroid.dropdashwallet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeFragment extends Fragment {

    ImageView menuDots, hideAndShowAmountToggle;
    SwipeRefreshLayout swipeRefresh;
    TextView walletBalance, walletRewardPoints;
    LinearLayout loadMoneyContainer, sendMoneyContainer;

    private boolean isAmountVisible = false;

    // 🔥 Store real values
    private long currentBalance = 0;
    private long currentRewardPoints = 0;

    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        menuDots = view.findViewById(R.id.menuDots);
        walletBalance = view.findViewById(R.id.walletBalance);
        walletRewardPoints = view.findViewById(R.id.walletRewardPoints);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        hideAndShowAmountToggle = view.findViewById(R.id.hideAndShowAmountToggle);

        loadMoneyContainer = view.findViewById(R.id.loadMoneyContainer);
        sendMoneyContainer = view.findViewById(R.id.sendMoneyContainer);

        prefs = requireContext().getSharedPreferences("wallet", Context.MODE_PRIVATE);

        // 🔥 Load saved toggle state
        isAmountVisible = prefs.getBoolean("showAmount", false);

        menuDots.setOnClickListener(this::showPopupMenu);

        swipeRefresh.setOnRefreshListener(this::fetchWalletData);

        sendMoneyContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent iNext = new Intent(requireContext(), EnterIdToSendActivity.class);
                startActivity(iNext);
            }
        });

        loadMoneyContainer.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoadMoneyActivity.class);
            startActivity(intent);
        });

        // 🔥 Toggle click
        hideAndShowAmountToggle.setOnClickListener(v -> {
            isAmountVisible = !isAmountVisible;

            prefs.edit().putBoolean("showAmount", isAmountVisible).apply();

            updateAmountUI();
        });

        // 🔥 Default UI
        updateAmountUI();

        // 🔥 Fetch data
        fetchWalletData();

        return view;
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);

        popupMenu.getMenu().add("Merchant Login");
        popupMenu.getMenu().add("Logout");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.equals("Merchant Login")) {

                // Open website
                String url = ApiConfig.getServerUrl(requireContext()) + "/register.html";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);

            } else if (title.equals("Logout")) {

                // Firebase logout
                FirebaseAuth.getInstance().signOut();

                Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();

                // Optional: redirect to log in activity
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            }

            return true;
        });

        popupMenu.show();
    }

    private void fetchWalletData() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            swipeRefresh.setRefreshing(false);
            return;
        }

        String uid = user.getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("wallets").child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    Long balance = snapshot.child("balance").getValue(Long.class);
                    Long rewardPoints = snapshot.child("rewardPoints").getValue(Long.class);

                    currentBalance = (balance != null) ? balance : 0;
                    currentRewardPoints = (rewardPoints != null) ? rewardPoints : 0;

                } else {
                    currentBalance = 0;
                    currentRewardPoints = 0;
                }

                updateAmountUI();

                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load wallet", Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void updateAmountUI() {

        walletBalance.animate().alpha(0f).setDuration(120).withEndAction(() -> {

            if (isAmountVisible) {

                walletBalance.setText(formatAmount(currentBalance));
                walletRewardPoints.setText(formatAmount(currentRewardPoints));
                hideAndShowAmountToggle.setImageResource(R.drawable.eye_opened);

            } else {

                walletBalance.setText("XXXX.XX");
                walletRewardPoints.setText("XXXX.XX");
                hideAndShowAmountToggle.setImageResource(R.drawable.eye_closed);
            }

            walletBalance.animate().alpha(1f).setDuration(120).start();
            walletRewardPoints.animate().alpha(1f).setDuration(120).start();

        }).start();
    }

    // 🔥 Number formatting (1,23,456)
    private String formatAmount(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("en", "IN"));
        return formatter.format(amount);
    }
}
