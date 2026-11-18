#!/vendor/bin/sh

# Mark the query as in-flight so callers never read a stale value from a
# previous run. init.esim.rc mirrors vendor.esim.state to sys.esim.state.
setprop vendor.esim.state querying

EID=$(/vendor/bin/mtkmtb -c 5:3 2>&1)

if echo "$EID" | grep -q "value=FAILED"; then
    STATE="disabled"
else
    STATE="enabled"
fi

setprop vendor.esim.state "$STATE"
