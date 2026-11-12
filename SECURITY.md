# Security Policy

## Scope

Nocturne Kernel Manager is an Android kernel-management application for
Samsung Galaxy M11 / A11 target devices.

The app uses a root shell only through the standard su -c interface. It does
not expose an arbitrary shell command field. Write operations are restricted
to a small list of known kernel nodes and validated values.

## Current write surfaces

- CPU cpufreq governor and scaling min/max values from a detected policy and
  its exposed OPP/governor lists.
- Block I/O scheduler values listed by the matching scheduler node.

GPU, voltage, regulator, CPR/corner, DDR overclock and thermal-protection
interfaces are not writable through the app.

## Reporting vulnerabilities

Do not publish exploitable security details in public issues. Use GitHub's
private vulnerability reporting for this repository if enabled. Otherwise,
contact the repository maintainer privately through GitHub.

Include affected version, device model, Android version, root manager, kernel
version, impact, and a safe reproduction description.

## Root authorization

Root permission is mediated by the installed root manager. Review the
permission prompt before granting access. Revoke the app's root permission
through that manager when it is no longer needed.
