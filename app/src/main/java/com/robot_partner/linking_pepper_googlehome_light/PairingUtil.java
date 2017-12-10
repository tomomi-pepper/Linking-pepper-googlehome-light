package com.robot_partner.linking_pepper_googlehome_light;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** ペアリングデータ操作ツールクラス **/
public class PairingUtil {

    /**
     * ペアリングセンサーから取得したoriginalDataを数値に変換する
     * @param type          ペアリングセンサータイプ情報
     * @param originalData  機器から取得した独自データ
     * @return              引数で与えられたoriginalDataをFloat型に変更した値
     */
    public static Float convertOriginalData(int type, byte[] originalData) {
        if (originalData != null) {
            if(originalData.length == 2) {
                // リトルエンディアン
                short data = ByteBuffer.wrap(originalData).order(ByteOrder.LITTLE_ENDIAN).getShort();

                if (type == 3) {
                    // 電池残量の場合
                    // 1つのバイト配列で充電要否フラグと電池残量を保持しているが、ここでは電池残量のみを返却する
                    return PairingUtil.calcFloatBattery((int) data);
                } else if (type == 4) {
                    // 温度データの場合（符号部1 指数部4 仮数部7）
                    return PairingUtil.calcFloat((int) data, 1, 4, 7);
                } else if (type == 5) {
                    // 湿度データの場合（符号部1 指数部4 仮数部7）
                    return PairingUtil.calcFloat((int) data, 0, 4, 8);
                }
            } else if(originalData.length == 4) {
                if (type == 6) {
                    // 気圧データの場合（符号部1 指数部8 仮数部23）
                    return ByteBuffer.wrap(originalData).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                }
            }
        }

        return null;
    }

    /**
     * originalDataをFloat型に変換する
     * @param data          数値変換後のoriginalData
     * @param signBit       符号部数
     * @param exponentBits  指数部数
     * @param fractionBits  仮数部数
     * @return              変換結果
     */
    public static Float calcFloat(final int data, final int signBit, final int exponentBits, final int fractionBits) {
        // float型: 符号部1 指数部8 仮数部23 のフォーマットに合わせる
        int bits = 0;

        // 符号部を設定
        if (signBit > 0) {
            bits |= ((data >> (exponentBits + fractionBits)) & 0x1) << 31;
        }

        // 指数部のマスクを作成
        // 例) 4bitなら、0xF(=1111)を作る
        int exponentMask = (1 << exponentBits) - 1;

        int exponent = (data >> fractionBits) & exponentMask;
        if (exponent == exponentMask) {
            // 指数部が全bit1の場合は無限大を指す
            exponent = 0xFF;
        } else if (exponent != 0) {
            exponent += (0xFF / 2) - (exponentMask / 2);
        }

        // 指数部を設定
        bits |= exponent << 23;

        // 仮数部のマスクを作成
        int fractionMask = (1 << fractionBits) - 1;

        // 仮数部を設定
        bits |= (data & fractionMask) << (23 - fractionBits);

        return Float.intBitsToFloat(bits);
    }

    /**
     * originalDataから電池残量を抽出する
     * @param data  数値変換後のoriginalData
     * @return      変換結果
     */
    private static Float calcFloatBattery(final int data) {
        // 上位1ビット : 充電要否フラグ
        // 下位11ビット : 電池残量（％） float変換後、10で除算する必要がある
        Float result;
        result = (float) (data & 0x7FF) / 10;

        return result;
    }

    /**
     * originalDataから充電要否フラグを抽出する
     * @param type          ペアリングセンサータイプ情報
     * @param originalData  機器から取得した独自データ
     * @return              充電要否フラグ
     */
    public static Boolean convertOriginalDataBatteryFlag(int type, byte[] originalData) {
        // 上位1ビット : 充電要否フラグ
        // 下位11ビット : 電池残量（％） float変換後、10で除算する必要がある

        // type=3以外では取得不能
        if (type != 3) {
            return null;
        }

        // リトルエンディアン
        short data = ByteBuffer.wrap(originalData).order(ByteOrder.LITTLE_ENDIAN).getShort();

        return ((data >> 11) & 0x1) == 0x1;
    }


    /**
     * originalDataから回数を抽出する
     * @param type          ペアリングセンサータイプ情報
     * @param originalData  機器から取得した独自データ
     * @return              回数
     */
    public static Integer convertOriginalDataCount(int type, byte[] originalData) {
        // 上位1ビット : フラグ
        // 下位11ビット : カウント

        // type=7,8,9以外では取得不能
        if ((type == 7) || (type == 8) || (type == 9)) {
            // リトルエンディアン
            short data = ByteBuffer.wrap(originalData).order(ByteOrder.LITTLE_ENDIAN).getShort();
            Integer result = (data & 0x7FF);

            return result;
        } else {
            return null;
        }
    }

    /**
     * originalDataからフラグを抽出する
     * @param type          ペアリングセンサータイプ情報
     * @param originalData  機器から取得した独自データ
     * @return              フラグ
     */
    public static Boolean convertOriginalDataFlag(int type, byte[] originalData) {
        // 上位1ビット : フラグ
        // 下位11ビット : カウント

        // type=7,8,9以外では取得不能
        if ((type == 7) || (type == 8) || (type == 9)) {
            // リトルエンディアン
            short data = ByteBuffer.wrap(originalData).order(ByteOrder.LITTLE_ENDIAN).getShort();

            return ((data >> 11) & 0x1) == 0x1;
        } else {
            return null;
        }
    }
}
