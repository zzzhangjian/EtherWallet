package com.jaeckel.etherwallet;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jaeckel.geth.EthereumJsonRpc.Callback;
import com.jaeckel.geth.json.EthAccountsResponse;
import com.jaeckel.geth.json.EthBlockNumberResponse;
import com.jaeckel.geth.json.EthGetBalanceResponse;
import com.jaeckel.geth.json.EthSyncingResponse;
import com.jaeckel.geth.json.EthSyncingResult;
import com.jaeckel.geth.json.NetPeerCountResponse;
import com.jaeckel.geth.json.PersonalListAccountsResponse;
import com.jaeckel.geth.json.PersonalNewAccountResponse;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;

import java.util.Locale;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static java.lang.String.format;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    //    private GethConnector gethConnector;
    private GethService gethService;
    private TextView currentBlock;
    private TextView highestBlock;
    private TextView netPeerCount;
    private TextView ethBalance;
    private TextView ethSyncing;
    private TextView ethAccount;

    private Subscription netPeerCountSubscription;
    private Subscription ethBlockNumberSubscription;
    private Subscription ethSyncingSubscription;

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            Timber.d("onServiceConnected()");
            GethService.MyBinder b = (GethService.MyBinder) binder;
            gethService = b.getService();

            ethSyncingSubscription = ethSyncingObservable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<EthSyncingResponse>() {
                        @Override
                        public void onCompleted() {
                            Timber.d("onCompleted()");
                            ethSyncing.setText(R.string.msg_syncing_done);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Timber.e(e, "onError(): ");
                            highestBlock.setText(format("ERROR: %s", e.getMessage()));
                        }

                        @Override
                        public void onNext(EthSyncingResponse ethSyncingResponse) {
                            Timber.d(format("OnNext(): %s", ethSyncingResponse));

                            EthSyncingResult result = ethSyncingResponse.result;
                            if (result != null) {
                                highestBlock.setText(format(Locale.ENGLISH, "Highest Block: %d", result.highestBlock));
                                ethSyncing.setText(R.string.msg_syncing_true);
                            } else {
                                ethSyncing.setText(R.string.msg_syncing_false);
                            }
                        }
                    });

            ethBlockNumberSubscription = ethBlockNumberObservable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<EthBlockNumberResponse>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            Timber.e(e, "Exception");
                        }

                        @Override
                        public void onNext(EthBlockNumberResponse ethBlockNumberResponse) {
                            currentBlock.setText(format(Locale.ENGLISH, "Current Block: %d", ethBlockNumberResponse.result));
                        }
                    });

            netPeerCountSubscription = netPeerCountObservable.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<NetPeerCountResponse>() {
                        @Override
                        public void onCompleted() {
                            Timber.d("onCompleted()");
                        }

                        @Override
                        public void onError(Throwable e) {
                            Timber.e(e, "onError(): ");
                            netPeerCount.setText(format(Locale.ENGLISH, "ERROR: %s", e.getMessage()));
                        }

                        @Override
                        public void onNext(NetPeerCountResponse netPeerCountResponse) {
                            Timber.d("onNext(): ");
                            netPeerCount.setText(format(Locale.ENGLISH, "NetPeerCount: %d", netPeerCountResponse.result));
                        }
                    });

        }

        public void onServiceDisconnected(ComponentName className) {
            gethService = null;
        }
    };

    Observable<EthSyncingResponse> ethSyncingObservable = Observable.create(
            new Observable.OnSubscribe<EthSyncingResponse>() {
                @Override
                public void call(final Subscriber<? super EthSyncingResponse> sub) {

                    Callback<EthSyncingResponse> ethSyncingCallback = new Callback<EthSyncingResponse>() {
                        @Override
                        public void onResult(EthSyncingResponse ethSyncingResult) {
                            Timber.d("onResult(): %s", ethSyncingResult);
                            sub.onNext(ethSyncingResult);
//                            sub.onCompleted();
                        }

                        @Override
                        public void onError(JSONRPC2Error error) {
                            Timber.d("onError(): %s", error.getMessage());
                        }
                    };
                    while (!sub.isUnsubscribed()) {
                        gethService.ethSyncing(ethSyncingCallback);
                        SystemClock.sleep(10000);
                    }
                }
            }
    );

    Observable<EthBlockNumberResponse> ethBlockNumberObservable = Observable.create(
            new Observable.OnSubscribe<EthBlockNumberResponse>() {
                @Override
                public void call(final Subscriber<? super EthBlockNumberResponse> sub) {

                    Callback<EthBlockNumberResponse> ethBlockNumberCallback = new Callback<EthBlockNumberResponse>() {
                        @Override
                        public void onResult(EthBlockNumberResponse ethSyncingResult) {
                            Timber.d("onResult(): %s", ethSyncingResult);
                            sub.onNext(ethSyncingResult);
//                            sub.onCompleted();
                        }

                        @Override
                        public void onError(JSONRPC2Error error) {
                            Timber.d("onError(): %s", error.getMessage());
                        }
                    };
                    while (!sub.isUnsubscribed()) {
                        gethService.ethBlockNumber(ethBlockNumberCallback);
                        SystemClock.sleep(2000);
                    }
                }
            }
    );

    Observable<NetPeerCountResponse> netPeerCountObservable = Observable.create(
            new Observable.OnSubscribe<NetPeerCountResponse>() {
                @Override
                public void call(final Subscriber<? super NetPeerCountResponse> sub) {

                    Callback<NetPeerCountResponse> netPeerCountcallback = new Callback<NetPeerCountResponse>() {
                        @Override
                        public void onResult(NetPeerCountResponse netPeerCount) {
                            Timber.d("onResult(): %s", netPeerCount);
                            sub.onNext(netPeerCount);
                        }

                        @Override
                        public void onError(JSONRPC2Error error) {
                            Timber.d("onError(): %s", error.getMessage());
                        }
                    };

                    while (!sub.isUnsubscribed()) {
                        gethService.netPeerCount(netPeerCountcallback);
                        SystemClock.sleep(2000);
                    }
                }
            }
    );

    Observable<EthAccountsResponse> ethAccountsObservable = Observable.create(
            new Observable.OnSubscribe<EthAccountsResponse>() {
                @Override
                public void call(final Subscriber<? super EthAccountsResponse> sub) {

                    Callback<EthAccountsResponse> ethAccountsCallback = new Callback<EthAccountsResponse>() {
                        @Override
                        public void onResult(EthAccountsResponse ethAccountsResponse) {
                            Timber.d("onResult(): %s", ethAccountsResponse);
                            sub.onNext(ethAccountsResponse);
                            sub.onCompleted();
                        }

                        @Override
                        public void onError(JSONRPC2Error error) {
                            Timber.d("onError(): %s", error.getMessage());
                        }
                    };

                    gethService.ethAccounts(ethAccountsCallback);
                }
            }
    );

    Observable<PersonalListAccountsResponse> personalListAccountsObservable = Observable.create(
            new Observable.OnSubscribe<PersonalListAccountsResponse>() {
                @Override
                public void call(final Subscriber<? super PersonalListAccountsResponse> sub) {

                    Callback<PersonalListAccountsResponse> ethAccountsCallback = new Callback<PersonalListAccountsResponse>() {
                        @Override
                        public void onResult(PersonalListAccountsResponse ethAccountsResponse) {
                            Timber.d("onResult(): %s", ethAccountsResponse);
                            sub.onNext(ethAccountsResponse);
                            sub.onCompleted();

//                            ethGetBalanceRespnseObservable.subscribeOn(Schedulers.io())
//                                    .observeOn(AndroidSchedulers.mainThread())
//                                    .subscribe();
                        }

                        @Override
                        public void onError(JSONRPC2Error error) {
                            Timber.d("onError(): %s", error.getMessage());
                        }
                    };

                    gethService.personalListAccounts(ethAccountsCallback);

                }
            }
    );

    Observable<EthGetBalanceResponse> ethGetBalanceRespnseObservable = Observable.create(
            new Observable.OnSubscribe<EthGetBalanceResponse>() {
                @Override
                public void call(final Subscriber<? super EthGetBalanceResponse> sub) {

                    Callback<EthGetBalanceResponse> ethAccountsCallback = new Callback<EthGetBalanceResponse>() {
                        @Override
                        public void onResult(EthGetBalanceResponse ethAccountsResponse) {
                            Timber.d("onResult(): %s", ethAccountsResponse);
                            sub.onNext(ethAccountsResponse);
                            sub.onCompleted();
                        }

                        @Override
                        public void onError(JSONRPC2Error error) {
                            Timber.d(error, "onError()");
                        }
                    };

                    gethService.ethGetBalance("fc175d4ebc50742899821ec95275f56d33dd5cd2", "latest", ethAccountsCallback);
                }
            }
    );

    Observable<PersonalNewAccountResponse> personalNewAccountObservable = Observable.create(
            new Observable.OnSubscribe<PersonalNewAccountResponse>() {
                @Override
                public void call(final Subscriber<? super PersonalNewAccountResponse> sub) {

                    Callback<PersonalNewAccountResponse> personalNewAccount = new Callback<PersonalNewAccountResponse>() {
                        @Override
                        public void onResult(PersonalNewAccountResponse ethAccountsResponse) {
                            Timber.d("onResult(): %s", ethAccountsResponse);
                            sub.onNext(ethAccountsResponse);
                            sub.onCompleted();
                        }

                        @Override
                        public void onError(JSONRPC2Error error) {
                            Timber.d(error, "onError()");
                        }
                    };

                    gethService.personalNewAccount("", personalNewAccount);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChainDataDir();
        setContentView(R.layout.activity_main);

        currentBlock = (TextView) findViewById(R.id.eth_current_block);
        highestBlock = (TextView) findViewById(R.id.highest_block);
        netPeerCount = (TextView) findViewById(R.id.net_peer_count);
        ethSyncing = (TextView) findViewById(R.id.eth_syncing);
        ethBalance = (TextView) findViewById(R.id.balances);
        ethAccount = (TextView) findViewById(R.id.account);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Timber.d("onClick()");
                ethAccountsObservable.subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<EthAccountsResponse>() {
                            @Override
                            public void onCompleted() {
                                Timber.d("onCompleted()");
                            }

                            @Override
                            public void onError(Throwable e) {
                                Timber.e(e, "onError(): ");
                                ethBalance.setText(format("ERROR: %s", e.getMessage()));
                            }

                            @Override
                            public void onNext(EthAccountsResponse ethAccountsResponse) {
                                Timber.d("onNext(): ethAccountsResponse: %s", ethAccountsResponse);
                                ethBalance.setText(format("Balances: %s", ethAccountsResponse.result));
                            }
                        });

                //TODO: create account
//                Geth.doAccountNew(getChainDataDir(), "password");
//                Geth.run("--datadir=" + getChainDataDir() + " account list");
//                Geth.run("account list");

                Snackbar.make(view, "Action...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    @NonNull
    private String getChainDataDir() {
        Timber.d("absolutePath: %s", getFilesDir().getAbsolutePath());
        return getFilesDir().getAbsolutePath();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_create_account) {
//            Geth.doAccountNew(getChainDataDir(), "password");
//            Snackbar.make(null, "Create Account...", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null)
//                    .show();
            return true;
        }
        if (id == R.id.action_list_accounts) {
            personalListAccountsObservable.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<PersonalListAccountsResponse>() {
                        @Override
                        public void onCompleted() {
                            Timber.d("onCompleted()");

                            ethGetBalanceRespnseObservable.subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Observer<EthGetBalanceResponse>() {
                                        @Override
                                        public void onCompleted() {

                                        }

                                        @Override
                                        public void onError(Throwable e) {

                                        }

                                        @Override
                                        public void onNext(EthGetBalanceResponse ethGetBalanceResponse) {
                                            Timber.i("ethGetBalanceResponse: %s", ethGetBalanceResponse);
                                            ethBalance.setText(EtherFormatter.formatWeiAsEther(ethGetBalanceResponse.result));
                                        }
                                    });
                        }

                        @Override
                        public void onError(Throwable e) {
                            Timber.e(e, "onError(): ");
                            ethBalance.setText(format("ERROR: %s", e.getMessage()));
                        }

                        @Override
                        public void onNext(PersonalListAccountsResponse personalListAccountsResponse) {
                            Timber.d("onNext(): personalListAccountsResponse: %s", personalListAccountsResponse);
                            ethAccount.setText(format("Balances: %s", personalListAccountsResponse.accounts.get(0)));
                        }
                    });
            return true;
        }

        if (id == R.id.action_new_account) {
            personalNewAccountObservable.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<PersonalNewAccountResponse>() {
                        @Override
                        public void onCompleted() {
                            Timber.d("onCompleted()");
                        }

                        @Override
                        public void onError(Throwable e) {
                            Timber.e(e, "onError(): ");
                            ethBalance.setText(format("ERROR: %s", e.getMessage()));
                        }

                        @Override
                        public void onNext(PersonalNewAccountResponse response) {
                            Timber.d("onNext(): response: %s",response);
                            ethBalance.setText(format("Balances: %s", response.toString()));
                        }
                    });

//            Snackbar.make(null, "List Accounts...", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null)
//                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.d("onResume()");
        Intent intent = new Intent(this, GethService.class);
        bindService(intent, mConnection,
                    Context.BIND_AUTO_CREATE
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        Timber.d("onPause()");

        Timber.d("netPeerCountSubscription.unsubscribe();");
        netPeerCountSubscription.unsubscribe();

        Timber.d("ethSyncingSubscription.unsubscribe();");
        ethSyncingSubscription.unsubscribe();
        ethBlockNumberSubscription.unsubscribe();

        unbindService(mConnection);
    }
}
