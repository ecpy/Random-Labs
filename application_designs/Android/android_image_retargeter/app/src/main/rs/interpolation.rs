#pragma version(1)
#pragma rs java_package_name(tech.silvermind.demo.retargter)
#pragma rs_fp_relaxed

rs_allocation domainIndexes;
rs_allocation mapIndexes;

rs_allocation bitmapSource;
rs_allocation bitmapOutput;
int sourceWidth;
int sourceHeight;
int targetWidth;
int targetHeight;

rs_script script;
rs_allocation nullPointer;

void root(const int32_t *domainIndex) {
   int domain_y = *domainIndex/targetWidth;
   int domain_x = *domainIndex%targetWidth;

   int32_t locationIndexInSource = rsGetElementAt_uint(mapIndexes, *domainIndex);
   int32_t source_y = locationIndexInSource/sourceWidth;
   int32_t source_x = locationIndexInSource%sourceWidth;

   rsSetElementAt_uchar4(bitmapOutput, rsGetElementAt_uchar4(bitmapSource, source_x, source_y), domain_x, domain_y);

}

void filter() {
    //rsForEach(script, domainIndexes, nullPointer);
}
