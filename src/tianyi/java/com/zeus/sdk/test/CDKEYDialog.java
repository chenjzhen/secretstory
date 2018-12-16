package com.zeus.sdk.test;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.movingstudio.secretstory.R;
import com.zeus.sdk.DataCallback;
import com.zeus.sdk.tools.SdkTools;

/**
 * Created by ben on 2018/10/10.
 */

public class CDKEYDialog extends Dialog {
    private final EditText mCdkey;

    public CDKEYDialog(Context context) {
        super(context, R.style.cdkey_dialog_style);
        setContentView(R.layout.dialog_cdkey);

        mCdkey = (EditText) findViewById(R.id.et_cdkey);
        Button sure = (Button) findViewById(R.id.btn_sure);
        sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取输入的兑换码
                String cdkey = mCdkey.getText().toString().trim();
                if (!TextUtils.isEmpty(cdkey)) {
                    // 调用兑换码接口
                    SdkTools.useRedemptionCode(cdkey, new DataCallback<String>() {
                        @Override
                        public void onSuccess(String productId) {
                            //兑换成功，根据商品id发放商品
                            Toast.makeText(getContext(), "兑换成功：" + productId, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailed(int errCode, String message) {
                            Toast.makeText(getContext(), "兑换失败：" + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                if (isShowing()) {
                    dismiss();
                }
            }
        });
        Button cancel = (Button) findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowing()) {
                    dismiss();
                }
            }
        });
    }
}
