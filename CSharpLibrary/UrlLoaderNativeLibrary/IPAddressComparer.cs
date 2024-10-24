using System;
using System.Collections.Generic;
using System.Net;

namespace UrlLoaderNativeLibrary;

public class IPAddressComparer : IComparer<IPAddress>
{
    public int Compare(IPAddress x, IPAddress y)
    {
        if (x == null || y == null)
            throw new ArgumentException("Cannot compare null IP addresses.");

        var xBytes = x.GetAddressBytes();
        var yBytes = y.GetAddressBytes();

        // Compare each byte in the arrays
        for (int i = 0; i < Math.Min(xBytes.Length, yBytes.Length); i++)
        {
            int comparison = xBytes[i].CompareTo(yBytes[i]);
            if (comparison != 0)
                return comparison;
        }

        // If all compared bytes are equal, the shorter address is considered smaller
        return xBytes.Length.CompareTo(yBytes.Length);
    }
}