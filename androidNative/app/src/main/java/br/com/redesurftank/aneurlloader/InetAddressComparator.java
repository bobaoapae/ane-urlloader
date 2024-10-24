package br.com.redesurftank.aneurlloader;

import java.net.InetAddress;
import java.util.Comparator;

public class InetAddressComparator implements Comparator<InetAddress> {
    @Override
    public int compare(InetAddress a1, InetAddress a2) {
        byte[] a1Bytes = a1.getAddress();
        byte[] a2Bytes = a2.getAddress();

        // Compare byte by byte
        for (int i = 0; i < Math.min(a1Bytes.length, a2Bytes.length); i++) {
            int comparison = Byte.compareUnsigned(a1Bytes[i], a2Bytes[i]);
            if (comparison != 0) {
                return comparison;
            }
        }

        // If all compared bytes are equal, compare the lengths of the addresses
        return Integer.compare(a1Bytes.length, a2Bytes.length);
    }
}
