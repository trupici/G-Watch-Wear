package sk.trupici.gwatch.wear.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.wear.remote.interactions.WatchFaceConfigIntentHelper;
import sk.trupici.gwatch.wear.R;

public class AnalogConfigActivity extends LocalizedActivityBase {

    private ViewPager2 viewPager;
    private PageAdapter pageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analog_settings_activity);

        WatchFaceConfigIntentHelper.getWatchFaceComponentExtra(getIntent());

        pageAdapter = new PageAdapter();
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(pageAdapter);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTabMode(TabLayout.MODE_AUTO);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText("OBJECT " + (position + 1))
        ).attach();
    }

    public class PageAdapter extends RecyclerView.Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_tab_test, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.getTextView().setText(Integer.toString(position+1));
        }

        @Override
        public int getItemCount() {
            return 10;
        }

// Taken from demo example
//
//        @NonNull
//        @Override
//        public Fragment createFragment(int position) {
//            // Return a NEW fragment instance in createFragment(int)
//            Fragment fragment = new DemoObjectFragment();
//            Bundle args = new Bundle();
//            // Our object is just an integer :-P
//            args.putInt(DemoObjectFragment.ARG_OBJECT, position + 1);
//            fragment.setArguments(args);
//            return fragment;
//        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
        }

        public TextView getTextView() {
            return textView;
        }
    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        overridePendingTransition(0, 0);
//    }


// Taken from demo example
//
//    public class CollectionDemoFragment extends Fragment {
//        // When requested, this adapter returns a DemoObjectFragment,
//        // representing an object in the collection.
//        DemoCollectionAdapter demoCollectionAdapter;
//        ViewPager2 viewPager;
//
//        @Override
//        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//            TabLayout tabLayout = view.findViewById(R.id.tab_layout);
//            new TabLayoutMediator(tabLayout, viewPager,
//                    (tab, position) -> tab.setText("OBJECT " + (position + 1))
//            ).attach();
//        }
//
//        @Nullable
//        @Override
//        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//            return inflater.inflate(R.layout.analog_settings_activity, container, false);
//        }
//
//        @Override
//        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//            demoCollectionAdapter = new DemoCollectionAdapter(this);
//            viewPager = view.findViewById(R.id.pager);
//            viewPager.setAdapter(demoCollectionAdapter);
//        }
//    }

//    public class DemoCollectionAdapter extends FragmentStateAdapter {
//        public DemoCollectionAdapter(Fragment fragment) {
//            super(fragment);
//        }
//
//        @NonNull
//        @Override
//        public Fragment createFragment(int position) {
//            // Return a NEW fragment instance in createFragment(int)
//            Fragment fragment = new DemoObjectFragment();
//            Bundle args = new Bundle();
//            // Our object is just an integer :-P
//            args.putInt(DemoObjectFragment.ARG_OBJECT, position + 1);
//            fragment.setArguments(args);
//            return fragment;
//        }
//
//        @Override
//        public int getItemCount() {
//            return 100;
//        }
//    }

    // Instances of this class are fragments representing a single
// object in our collection.
//    public class DemoObjectFragment extends Fragment {
//        public static final String ARG_OBJECT = "object";
//
//        @Nullable
//        @Override
//        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                                 @Nullable Bundle savedInstanceState) {
//            return inflater.inflate(R.layout.layout_tab_test, container, false);
//        }
//
//        @Override
//        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//            Bundle args = getArguments();
//            ((TextView) view.findViewById(android.R.id.text1)).setText(Integer.toString(args.getInt(ARG_OBJECT)));
//        }
//    }
}