extern crate proc_macro;
use proc_macro::TokenStream;
use quote::quote;
use syn::export::ToTokens;
use syn::parse_macro_input;

// #[proc_macro_attribute]
// pub fn btp(_attr: TokenStream, item: TokenStream) -> TokenStream {
//     let mut input = parse_macro_input!(item as syn::ItemImpl);
//     // for item in input.items.into_iter().filter(|item| match item {
//     //     syn::ImplItem::Method(item) => {
//     //         if item
//     //             .attrs
//     //             .iter()
//     //             .any(|attr| attr.path.to_token_stream().to_string().as_str() == "owner")
//     //         {
//     //             return true;
//     //         }
//     //         false
//     //     }
//     //     _ => false,
//     // }) {
//     //     let mut input = parse_macro_input!(item as syn::ImplItemMethod);
//     //     input.block.stmts.insert(
//     //         0,
//     //         syn::parse(
//     //             quote! {
//     //                 assert!(self.owners.has("".to_string()), "Test");
//     //             }
//     //             .into(),
//     //         )
//     //         .unwrap(),
//     //     );
//     // }
//     let token_stream = quote! {
//         #input
//     };
//     token_stream.into()
// }

#[proc_macro_attribute]
pub fn owner(_attr: TokenStream, item: TokenStream) -> TokenStream {
    let mut input = parse_macro_input!(item as syn::ItemFn);
    input.block.stmts.insert(
        0,
        syn::parse(
            quote! {
                self.has_permission();
            }
            .into(),
        )
        .unwrap(),
    );
    let token_stream = quote! {
        #input
    };
    token_stream.into()
}
